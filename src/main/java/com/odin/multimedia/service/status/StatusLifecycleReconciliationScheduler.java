package com.odin.multimedia.service.status;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusReconciliationState;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.StatusDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Component @RequiredArgsConstructor
public class StatusLifecycleReconciliationScheduler {
    private final StatusCatalogRepository repository;
    private final StatusCatalogPersistenceService persistence;
    private final StatusMediaCleanupService mediaCleanup;
    private final AuthoritativeStatusDeletionService deletionService;
    private final StatusDeletionService legacyDeletionService;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();

    @Value("${status.lifecycle-reconciliation.enabled:true}") private boolean enabled;
    @Value("${status.lifecycle-reconciliation.batch-size:100}") private int configuredBatchSize;

    @Scheduled(fixedDelayString = "${status.lifecycle-reconciliation.interval-ms:60000}")
    public void reconcile() {
        if (!enabled || !running.compareAndSet(false, true)) return;
        long started = System.nanoTime();
        int transitioned = 0, expirySuccess = 0, expiryFailure = 0, deletionSuccess = 0;
        try {
            int batch = Math.max(1, Math.min(configuredBatchSize, 500));
            Instant now = clock.instant();
            List<StatusCatalogRecord> due = repository
                    .findByLifecycleStateAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                            StatusLifecycleState.ACTIVE, now, PageRequest.of(0, batch));
            for (StatusCatalogRecord candidate : due) {
                try {
                    StatusCatalogRecord expired = persistence.expireIfDue(candidate.getStatusId(), now);
                    if (expired.getLifecycleState() != StatusLifecycleState.EXPIRED) continue;
                    transitioned++;
                    if (cleanupExpired(expired)) expirySuccess++; else expiryFailure++;
                    if (expired.getLegacyStatusKey() != null) {
                        legacyDeletionService.deleteCompatibilityStatus(expired.getUploaderId(), expired.getLegacyStatusKey());
                    }
					legacyDeletionService.publishStatusDeleteNotifications(expired.getUploaderId(),
							expired.getStatusId(), expired.getLegacyStatusKey(), "STATUS_EXPIRED");
                } catch (RuntimeException conflictOrFailure) {
                    log.warn("Status expiry transition skipped. statusId={}, category=TRANSITION_CONFLICT",
                            candidate.getStatusId());
                }
            }
            List<StatusCatalogRecord> pendingExpired = repository
                    .findByLifecycleStateAndReconciliationStateOrderByUpdatedTimestampAsc(
                            StatusLifecycleState.EXPIRED, StatusReconciliationState.PENDING, PageRequest.of(0, batch));
            for (StatusCatalogRecord expired : pendingExpired) {
                if (cleanupExpired(expired)) expirySuccess++; else expiryFailure++;
            }
            deletionSuccess = deletionService.retryPending(batch);
            log.info("Status lifecycle reconciliation completed. selectedExpired={}, transitioned={}, expiryCleanupSuccess={}, expiryCleanupFailure={}, deletionCleanupSuccess={}, durationMs={}",
                    due.size(), transitioned, expirySuccess, expiryFailure, deletionSuccess,
                    (System.nanoTime() - started) / 1_000_000L);
        } finally {
            running.set(false);
        }
    }

    boolean cleanupExpired(StatusCatalogRecord record) {
        if (mediaCleanup.cleanup(record.getMediaReference())) {
            persistence.completeExpiredCleanup(record.getStatusId());
            return true;
        }
        persistence.markExpiredCleanupPending(record.getStatusId());
        return false;
    }

    public void setEnabledForTest(boolean value) { enabled = value; }
    public void setBatchSizeForTest(int value) { configuredBatchSize = value; }
}
