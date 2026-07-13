package com.odin.multimedia.service.status;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.odin.multimedia.config.StatusDurationProperties;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.IdempotencyProvenance;
import com.odin.multimedia.enums.StatusFailureReason;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusMediaType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusUploadLifecycleService {

    private final StatusCatalogPersistenceService catalog;
    private final StatusMediaStore mediaStore;
    private final StatusDurationProperties durationProperties;
    private final Clock clock;

    public StatusUploadResult upload(String uploaderId, StatusMediaType mediaType, byte[] content,
            String requestedIdempotencyKey) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("Status media is required");

        String checksum = StatusMediaChecksum.sha256(content);
        boolean clientSupplied = hasText(requestedIdempotencyKey);
        String idempotencyKey = clientSupplied ? requestedIdempotencyKey.trim() : UUID.randomUUID().toString();
        if (idempotencyKey.length() > 128) throw new IllegalArgumentException("Idempotency key is too long");

        Optional<StatusCatalogRecord> existing = catalog.findByIdempotencyKey(uploaderId, idempotencyKey);
        if (existing.isPresent()) return resolveDuplicate(existing.get(), mediaType, checksum);

        Instant createdAt = clock.instant();
        StatusCatalogRecord uploading;
        try {
            uploading = catalog.createUploading(uploaderId, mediaType, createdAt,
                    createdAt.plus(durationProperties.getDuration()), idempotencyKey,
                    clientSupplied ? IdempotencyProvenance.CLIENT : IdempotencyProvenance.LEGACY_GENERATED);
        } catch (DataIntegrityViolationException race) {
            return resolveDuplicate(catalog.findByIdempotencyKey(uploaderId, idempotencyKey)
                    .orElseThrow(() -> race), mediaType, checksum);
        }

        String mediaReference = null;
        StatusFailureReason currentFailure = StatusFailureReason.MEDIA_STORE_FAILED;
        try {
            StatusMediaCommitResult committed = mediaStore.store(new StatusMediaStoreRequest(content, mediaType));
            mediaReference = committed.getMediaReference();
            currentFailure = StatusFailureReason.MEDIA_VERIFICATION_FAILED;
            if (!checksum.equalsIgnoreCase(committed.getChecksum())) {
                throw new StatusUploadException(StatusFailureReason.CHECKSUM_MISMATCH,
                        "Stored media checksum does not match uploaded media");
            }
            StatusMediaVerificationResult verification = mediaStore.verify(mediaReference, checksum);
            if (!verification.isValid()) {
                throw new StatusUploadException(StatusFailureReason.MEDIA_VERIFICATION_FAILED,
                        "Stored media could not be verified");
            }
            currentFailure = StatusFailureReason.CATALOG_PERSISTENCE_FAILED;
            catalog.markMediaReady(uploading.getStatusId(), mediaReference, checksum);
            currentFailure = StatusFailureReason.ACTIVATION_FAILED;
            return new StatusUploadResult(catalog.activate(uploading.getStatusId()), true);
        } catch (RuntimeException failure) {
            safeDelete(mediaReference);
            StatusFailureReason reason = failure instanceof StatusUploadException
                    ? ((StatusUploadException) failure).getReason() : currentFailure;
            safeMarkFailed(uploading.getStatusId(), reason);
            throw failure;
        }
    }

    public StatusCatalogRecord attachLegacyStatusKey(String statusId, String legacyStatusKey) {
        return catalog.attachLegacyStatusKey(statusId, legacyStatusKey);
    }

    private StatusUploadResult resolveDuplicate(StatusCatalogRecord record, StatusMediaType mediaType,
            String checksum) {
        if (record.getMediaType() != mediaType) {
            throw new IllegalStateException("Idempotency key was already used for another media type");
        }
        if (record.getLifecycleState() != StatusLifecycleState.ACTIVE) {
            throw new IllegalStateException("An upload with this idempotency key is still in progress or failed");
        }
        if (!checksum.equalsIgnoreCase(record.getChecksum())) {
            throw new IllegalStateException("Idempotency key was already used for different media");
        }
        return new StatusUploadResult(record, false);
    }

    private void safeDelete(String mediaReference) {
        if (mediaReference == null) return;
        try {
            mediaStore.delete(mediaReference);
        } catch (RuntimeException cleanupFailure) {
            log.error("Failed to clean up uncommitted status media. mediaReference={}", mediaReference,
                    cleanupFailure);
        }
    }

    private void safeMarkFailed(String statusId, StatusFailureReason reason) {
        try {
            catalog.markFailed(statusId, reason);
        } catch (RuntimeException persistenceFailure) {
            log.error("Failed to persist status upload failure. statusId={}", statusId, persistenceFailure);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class StatusUploadException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        private final StatusFailureReason reason;

        private StatusUploadException(StatusFailureReason reason, String message) {
            super(message);
            this.reason = reason;
        }

        private StatusFailureReason getReason() { return reason; }
    }
}
