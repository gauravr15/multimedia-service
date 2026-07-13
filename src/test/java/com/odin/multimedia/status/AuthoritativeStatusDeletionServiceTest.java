package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.StatusDeletionService;
import com.odin.multimedia.service.status.*;

class AuthoritativeStatusDeletionServiceTest {
    @Test void ownerDeletionTransitionsAndDeletesOnlySelectedReference() {
        StatusCatalogRecord record = active("7", "selected.jpg");
        StatusCatalogPersistenceService persistence = mock(StatusCatalogPersistenceService.class);
        StatusMediaCleanupService store = mock(StatusMediaCleanupService.class);
        when(store.cleanup("selected.jpg")).thenReturn(true);
        when(persistence.beginDeletion(record.getStatusId(), "7")).thenAnswer(i -> { record.transitionTo(StatusLifecycleState.DELETING); return record; });
        when(persistence.completeDeletion(record.getStatusId())).thenAnswer(i -> { record.transitionTo(StatusLifecycleState.DELETED); return record; });
        AuthoritativeStatusDeletionService service = new AuthoritativeStatusDeletionService(persistence,
                mock(StatusCatalogRepository.class), store, mock(StatusDeletionService.class));
        assertEquals(StatusLifecycleState.DELETED, service.delete("7", record.getStatusId()).getState());
        verify(store).cleanup("selected.jpg");
        verify(store, never()).cleanup("sibling.jpg");
    }

    @Test void cleanupFailureRemainsDeletingAndRetryable() {
        StatusCatalogRecord record = active("7", "selected.jpg");
        StatusCatalogPersistenceService persistence = mock(StatusCatalogPersistenceService.class);
        StatusMediaCleanupService store = mock(StatusMediaCleanupService.class);
        when(persistence.beginDeletion(record.getStatusId(), "7")).thenAnswer(i -> { record.transitionTo(StatusLifecycleState.DELETING); return record; });
        when(store.cleanup("selected.jpg")).thenReturn(false);
        AuthoritativeStatusDeletionService service = new AuthoritativeStatusDeletionService(persistence,
                mock(StatusCatalogRepository.class), store, mock(StatusDeletionService.class));
        assertEquals(StatusLifecycleState.DELETING, service.delete("7", record.getStatusId()).getState());
        verify(persistence).markDeletionCleanupPending(record.getStatusId());
        assertEquals(StatusLifecycleState.DELETING, record.getLifecycleState());
    }

    @Test void persistenceRejectsNonOwnerWithoutMediaCleanup() {
        StatusCatalogPersistenceService persistence = mock(StatusCatalogPersistenceService.class);
        StatusMediaCleanupService store = mock(StatusMediaCleanupService.class);
        when(persistence.beginDeletion(anyString(), eq("8"))).thenThrow(new StatusDeletionNotFoundException());
        AuthoritativeStatusDeletionService service = new AuthoritativeStatusDeletionService(persistence,
                mock(StatusCatalogRepository.class), store, mock(StatusDeletionService.class));
        assertThrows(StatusDeletionNotFoundException.class,
                () -> service.delete("8", "11111111-1111-1111-1111-111111111111"));
        verifyNoInteractions(store);
    }

    private StatusCatalogRecord active(String owner, String reference) {
        Instant now = Instant.parse("2026-07-12T00:00:00Z");
        StatusCatalogRecord record = new StatusCatalogRecord(owner, StatusMediaType.IMAGE, now,
                now.plusSeconds(86400), "key", IdempotencyProvenance.CLIENT);
        record.attachVerifiedMedia(reference, "a".repeat(64));
        record.transitionTo(StatusLifecycleState.MEDIA_READY);
        record.transitionTo(StatusLifecycleState.ACTIVE);
        return record;
    }
}
