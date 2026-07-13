package com.odin.multimedia.entity.status;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import com.odin.multimedia.enums.IdempotencyProvenance;
import com.odin.multimedia.enums.StatusFailureReason;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.enums.StatusReconciliationState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "status_catalog",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_status_catalog_uploader_idempotency",
                        columnNames = {"uploader_id", "idempotency_key"}),
                @UniqueConstraint(name = "uk_status_catalog_legacy_key", columnNames = "legacy_status_key")
        },
        indexes = {
                @Index(name = "idx_status_catalog_uploader_state_created",
                        columnList = "uploader_id,lifecycle_state,created_at"),
                @Index(name = "idx_status_catalog_state_expires", columnList = "lifecycle_state,expires_at"),
                @Index(name = "idx_status_catalog_feed_seek",
                        columnList = "lifecycle_state,created_at,status_id"),
                @Index(name = "idx_status_catalog_reconciliation_updated",
                        columnList = "reconciliation_state,updated_timestamp")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusCatalogRecord {

    @Id
    @Column(name = "status_id", nullable = false, updatable = false, length = 36)
    private String statusId;

    @Column(name = "uploader_id", nullable = false, updatable = false, length = 36)
    private String uploaderId;

    @Column(name = "legacy_status_key", unique = true, length = 255)
    private String legacyStatusKey;

    @Column(name = "media_reference", length = 1024)
    private String mediaReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, updatable = false, length = 16)
    private StatusMediaType mediaType;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 32)
    private StatusLifecycleState lifecycleState;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 64)
    private StatusFailureReason failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_state", nullable = false, length = 32)
    private StatusReconciliationState reconciliationState;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "idempotency_provenance", nullable = false, updatable = false, length = 32)
    private IdempotencyProvenance idempotencyProvenance;

    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private Instant createdTimestamp;

    @Column(name = "updated_timestamp", nullable = false)
    private Instant updatedTimestamp;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public StatusCatalogRecord(String uploaderId, StatusMediaType mediaType, Instant createdAt,
            Instant expiresAt, String idempotencyKey, IdempotencyProvenance provenance) {
        this.statusId = UUID.randomUUID().toString();
        this.uploaderId = requireText(uploaderId, "uploaderId");
        this.mediaType = requireNonNull(mediaType, "mediaType");
        this.createdAt = requireNonNull(createdAt, "createdAt");
        this.expiresAt = requireNonNull(expiresAt, "expiresAt");
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        this.idempotencyProvenance = requireNonNull(provenance, "idempotencyProvenance");
        this.lifecycleState = StatusLifecycleState.UPLOADING;
        this.reconciliationState = StatusReconciliationState.NONE;
        validateExpiry();
    }

    public void attachVerifiedMedia(String reference, String sha256) {
        this.mediaReference = requireText(reference, "mediaReference");
        this.checksum = requireSha256(sha256);
    }

    public void attachLegacyStatusKey(String legacyKey) {
        if (this.legacyStatusKey != null) throw new IllegalStateException("legacyStatusKey is already assigned");
        this.legacyStatusKey = requireText(legacyKey, "legacyStatusKey");
    }

    public void transitionTo(StatusLifecycleState target) {
        StatusLifecyclePolicy.validateTransition(this, target);
        this.lifecycleState = target;
        if (target == StatusLifecycleState.FAILED && failureReason == null) {
            failureReason = StatusFailureReason.UNKNOWN;
        }
    }

    public void markFailed(StatusFailureReason reason) {
        this.failureReason = requireNonNull(reason, "failureReason");
        transitionTo(StatusLifecycleState.FAILED);
    }

    public void setReconciliationState(StatusReconciliationState state) {
        this.reconciliationState = requireNonNull(state, "reconciliationState");
    }

    void validateActivation() {
        requireText(statusId, "statusId");
        requireText(uploaderId, "uploaderId");
        requireText(mediaReference, "mediaReference");
        requireSha256(checksum);
        requireNonNull(mediaType, "mediaType");
        requireNonNull(createdAt, "createdAt");
        requireNonNull(expiresAt, "expiresAt");
        requireText(idempotencyKey, "idempotencyKey");
        validateExpiry();
    }

    private void validateExpiry() {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalStateException("expiresAt must be after createdAt");
        }
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (statusId == null) statusId = UUID.randomUUID().toString();
        if (lifecycleState == null) lifecycleState = StatusLifecycleState.UPLOADING;
        if (reconciliationState == null) reconciliationState = StatusReconciliationState.NONE;
        createdTimestamp = now;
        updatedTimestamp = now;
        validateExpiry();
    }

    @PreUpdate
    void beforeUpdate() {
        updatedTimestamp = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException(field + " is required");
        return value;
    }

    private static String requireSha256(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("checksum must be a SHA-256 hex value");
        }
        return value.toLowerCase();
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) throw new IllegalStateException(field + " is required");
        return value;
    }
}
