package com.odin.multimedia.service.status;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.IdempotencyProvenance;
import com.odin.multimedia.enums.StatusFailureReason;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.enums.StatusReconciliationState;
import com.odin.multimedia.repository.StatusCatalogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusCatalogPersistenceService {

    private final StatusCatalogRepository repository;

    @Transactional(readOnly = true)
    public Optional<StatusCatalogRecord> findByIdempotencyKey(String uploaderId, String idempotencyKey) {
        return repository.findByUploaderIdAndIdempotencyKey(uploaderId, idempotencyKey);
    }

    @Transactional
    public StatusCatalogRecord createUploading(String uploaderId, StatusMediaType mediaType,
            Instant createdAt, Instant expiresAt, String idempotencyKey, IdempotencyProvenance provenance) {
        StatusCatalogRecord record = repository.saveAndFlush(new StatusCatalogRecord(
                uploaderId, mediaType, createdAt, expiresAt, idempotencyKey, provenance));
        log.info("Status lifecycle transition. statusId={}, uploaderId={}, mediaType={}, state=UPLOADING, provenance={}",
                record.getStatusId(), uploaderId, mediaType, provenance);
        return record;
    }

    @Transactional
    public StatusCatalogRecord markMediaReady(String statusId, String mediaReference, String checksum) {
        StatusCatalogRecord record = requireRecord(statusId);
        record.attachVerifiedMedia(mediaReference, checksum);
        record.transitionTo(StatusLifecycleState.MEDIA_READY);
        StatusCatalogRecord saved = repository.saveAndFlush(record);
        log.info("Status lifecycle transition. statusId={}, uploaderId={}, mediaType={}, state=MEDIA_READY",
                saved.getStatusId(), saved.getUploaderId(), saved.getMediaType());
        return saved;
    }

    @Transactional
    public StatusCatalogRecord activate(String statusId) {
        StatusCatalogRecord record = requireRecord(statusId);
        record.transitionTo(StatusLifecycleState.ACTIVE);
        StatusCatalogRecord saved = repository.saveAndFlush(record);
        log.info("Status lifecycle transition. statusId={}, uploaderId={}, mediaType={}, state=ACTIVE",
                saved.getStatusId(), saved.getUploaderId(), saved.getMediaType());
        return saved;
    }

    @Transactional
    public StatusCatalogRecord attachLegacyStatusKey(String statusId, String legacyStatusKey) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLegacyStatusKey() == null) {
            record.attachLegacyStatusKey(legacyStatusKey);
            return repository.saveAndFlush(record);
        }
        return record;
    }

    @Transactional
    public void markFailed(String statusId, StatusFailureReason reason) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.UPLOADING
                || record.getLifecycleState() == StatusLifecycleState.MEDIA_READY) {
            record.markFailed(reason);
            record.setReconciliationState(StatusReconciliationState.PENDING);
            repository.saveAndFlush(record);
            log.warn("Status lifecycle transition. statusId={}, uploaderId={}, mediaType={}, state=FAILED, reason={}",
                    record.getStatusId(), record.getUploaderId(), record.getMediaType(), reason);
        }
    }

    @Transactional
    public StatusCatalogRecord beginDeletion(String statusId, String ownerId) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (!record.getUploaderId().equals(ownerId)) throw new StatusDeletionNotFoundException();
        if (record.getLifecycleState() == StatusLifecycleState.ACTIVE) {
            record.transitionTo(StatusLifecycleState.DELETING);
            record.setReconciliationState(StatusReconciliationState.PENDING);
            return repository.saveAndFlush(record);
        }
        if (record.getLifecycleState() == StatusLifecycleState.DELETING
                || record.getLifecycleState() == StatusLifecycleState.DELETED) return record;
        throw new StatusDeletionNotFoundException();
    }

    @Transactional
    public StatusCatalogRecord completeDeletion(String statusId) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.DELETING) {
            record.transitionTo(StatusLifecycleState.DELETED);
            record.setReconciliationState(StatusReconciliationState.COMPLETED);
            return repository.saveAndFlush(record);
        }
        return record;
    }

    @Transactional
    public void markDeletionCleanupPending(String statusId) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.DELETING) {
            record.setReconciliationState(StatusReconciliationState.PENDING);
            repository.saveAndFlush(record);
        }
    }

    @Transactional
    public StatusCatalogRecord expireIfDue(String statusId, Instant now) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.ACTIVE
                && !now.isBefore(record.getExpiresAt())) {
            record.transitionTo(StatusLifecycleState.EXPIRED);
            record.setReconciliationState(StatusReconciliationState.PENDING);
            return repository.saveAndFlush(record);
        }
        return record;
    }

    @Transactional
    public void completeExpiredCleanup(String statusId) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.EXPIRED) {
            record.setReconciliationState(StatusReconciliationState.COMPLETED);
            repository.saveAndFlush(record);
        }
    }

    @Transactional
    public void markExpiredCleanupPending(String statusId) {
        StatusCatalogRecord record = requireRecord(statusId);
        if (record.getLifecycleState() == StatusLifecycleState.EXPIRED) {
            record.setReconciliationState(StatusReconciliationState.PENDING);
            repository.saveAndFlush(record);
        }
    }

    private StatusCatalogRecord requireRecord(String statusId) {
        return repository.findById(statusId)
                .orElseThrow(() -> new IllegalStateException("Status catalog record not found"));
    }
}
