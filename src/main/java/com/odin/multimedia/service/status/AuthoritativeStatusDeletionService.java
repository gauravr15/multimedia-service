package com.odin.multimedia.service.status;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.odin.multimedia.dto.StatusDeletionResponse;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusReconciliationState;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.StatusDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class AuthoritativeStatusDeletionService {
    private final StatusCatalogPersistenceService persistence;
    private final StatusCatalogRepository repository;
    private final StatusMediaCleanupService mediaCleanupService;
    private final StatusDeletionService legacyDeletionService;

    public StatusDeletionResponse delete(String ownerId, String statusId) {
        if (statusId == null || !statusId.matches("[0-9a-fA-F-]{36}")) throw new StatusDeletionNotFoundException();
        StatusCatalogRecord record = persistence.beginDeletion(statusId, ownerId);
        if (record.getLifecycleState() == StatusLifecycleState.DELETED) {
            return response(record);
        }
        StatusCatalogRecord completed = cleanup(record);
        if (record.getLegacyStatusKey() != null) {
            legacyDeletionService.deleteCompatibilityStatus(ownerId, record.getLegacyStatusKey());
        }
		legacyDeletionService.publishStatusDeleteNotifications(ownerId, record.getStatusId(),
				record.getLegacyStatusKey(), "STATUS_DELETED");
        return response(completed);
    }

    public int retryPending(int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        List<StatusCatalogRecord> pending = repository.findByLifecycleStateAndReconciliationState(
                StatusLifecycleState.DELETING, StatusReconciliationState.PENDING, PageRequest.of(0, bounded));
        int completed = 0;
        for (StatusCatalogRecord record : pending) {
            if (cleanup(record).getLifecycleState() == StatusLifecycleState.DELETED) completed++;
        }
        return completed;
    }

    private StatusCatalogRecord cleanup(StatusCatalogRecord record) {
        try {
            if (!mediaCleanupService.cleanup(record.getMediaReference())) throw new IllegalStateException();
            StatusCatalogRecord completed = persistence.completeDeletion(record.getStatusId());
            log.info("Status deletion cleanup completed. statusId={}, ownerId={}, state=DELETED",
                    record.getStatusId(), record.getUploaderId());
            return completed;
        } catch (RuntimeException cleanupFailure) {
            persistence.markDeletionCleanupPending(record.getStatusId());
            log.warn("Status deletion cleanup pending. statusId={}, ownerId={}, state=DELETING",
                    record.getStatusId(), record.getUploaderId());
            return record;
        }
    }

    private StatusDeletionResponse response(StatusCatalogRecord record) {
        return new StatusDeletionResponse(record.getStatusId(), record.getLifecycleState());
    }
}
