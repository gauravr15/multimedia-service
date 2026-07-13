package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.StatusDeletionService;
import com.odin.multimedia.service.status.*;

class StatusExpiryReconciliationSchedulerTest {
    @Test void transitionsBeforeCleanupAndCompletesExpiredMedia() {
        Instant now=Instant.parse("2026-07-12T12:00:00Z");
        StatusCatalogRecord record=active(now.minusSeconds(1));
        StatusCatalogRepository repo=mock(StatusCatalogRepository.class);
        StatusCatalogPersistenceService persistence=mock(StatusCatalogPersistenceService.class);
        StatusMediaCleanupService cleanup=mock(StatusMediaCleanupService.class);
        when(repo.findByLifecycleStateAndExpiresAtLessThanEqualOrderByExpiresAtAsc(any(),any(),any())).thenReturn(Collections.singletonList(record));
        when(repo.findByLifecycleStateAndReconciliationStateOrderByUpdatedTimestampAsc(any(),any(),any())).thenReturn(Collections.emptyList());
        when(persistence.expireIfDue(record.getStatusId(),now)).thenAnswer(i->{record.transitionTo(StatusLifecycleState.EXPIRED);return record;});
        when(cleanup.cleanup("media.jpg")).thenReturn(true);
        StatusLifecycleReconciliationScheduler scheduler=new StatusLifecycleReconciliationScheduler(repo,persistence,cleanup,
                mock(AuthoritativeStatusDeletionService.class),mock(StatusDeletionService.class),Clock.fixed(now,ZoneOffset.UTC));
        scheduler.setEnabledForTest(true); scheduler.setBatchSizeForTest(10); scheduler.reconcile();
        assertEquals(StatusLifecycleState.EXPIRED,record.getLifecycleState());
        verify(persistence).completeExpiredCleanup(record.getStatusId());
    }

    @Test void cleanupFailureLeavesExpiredPendingAndDisabledDoesNothing() {
        Instant now=Instant.parse("2026-07-12T12:00:00Z");
        StatusCatalogRecord record=active(now.minusSeconds(1)); record.transitionTo(StatusLifecycleState.EXPIRED);
        StatusCatalogRepository repo=mock(StatusCatalogRepository.class); StatusCatalogPersistenceService persistence=mock(StatusCatalogPersistenceService.class);
        StatusMediaCleanupService cleanup=mock(StatusMediaCleanupService.class); when(cleanup.cleanup("media.jpg")).thenReturn(false);
        StatusLifecycleReconciliationScheduler scheduler=new StatusLifecycleReconciliationScheduler(repo,persistence,cleanup,
                mock(AuthoritativeStatusDeletionService.class),mock(StatusDeletionService.class),Clock.fixed(now,ZoneOffset.UTC));
        scheduler.setEnabledForTest(true); scheduler.setBatchSizeForTest(10);
        when(repo.findByLifecycleStateAndExpiresAtLessThanEqualOrderByExpiresAtAsc(any(),any(),any())).thenReturn(Collections.emptyList());
        when(repo.findByLifecycleStateAndReconciliationStateOrderByUpdatedTimestampAsc(any(),any(),any())).thenReturn(Collections.singletonList(record));
        scheduler.reconcile(); verify(persistence).markExpiredCleanupPending(record.getStatusId());
        scheduler.setEnabledForTest(false); scheduler.reconcile(); verifyNoMoreInteractions(persistence);
    }

    private StatusCatalogRecord active(Instant expires) {
        Instant created=expires.minusSeconds(60); StatusCatalogRecord r=new StatusCatalogRecord("2",StatusMediaType.IMAGE,created,expires,"k",IdempotencyProvenance.CLIENT);
        r.attachVerifiedMedia("media.jpg","a".repeat(64)); r.transitionTo(StatusLifecycleState.MEDIA_READY); r.transitionTo(StatusLifecycleState.ACTIVE); return r;
    }
}
