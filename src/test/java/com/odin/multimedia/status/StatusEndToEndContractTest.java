package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.dto.StatusFeedPage;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.status.*;

class StatusEndToEndContractTest {
    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

    @Test void mixedLifecycleMatrixReturnsOnlyIndependentActiveImageAndVideo() {
        StatusCatalogRecord activeImage = record(StatusMediaType.IMAGE, NOW.minusSeconds(4), NOW.plusSeconds(40));
        StatusCatalogRecord activeVideo = record(StatusMediaType.VIDEO, NOW.minusSeconds(3), NOW.plusSeconds(30));
        StatusCatalogRecord deletedImage = record(StatusMediaType.IMAGE, NOW.minusSeconds(2), NOW.plusSeconds(20));
        deletedImage.transitionTo(StatusLifecycleState.DELETING);
        deletedImage.transitionTo(StatusLifecycleState.DELETED);
        StatusCatalogRecord expiredVideo = record(StatusMediaType.VIDEO, NOW.minusSeconds(1), NOW);
        expiredVideo.transitionTo(StatusLifecycleState.EXPIRED);

        Set<String> ids = new HashSet<>(Arrays.asList(activeImage.getStatusId(), activeVideo.getStatusId(),
                deletedImage.getStatusId(), expiredVideo.getStatusId()));
        assertEquals(4, ids.size());
        assertEquals(4, new HashSet<>(Arrays.asList(activeImage.getMediaReference(), activeVideo.getMediaReference(),
                deletedImage.getMediaReference(), expiredVideo.getMediaReference())).size());

        StatusCatalogRepository repository = mock(StatusCatalogRepository.class);
        when(repository.findFeedCandidates(any(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList(activeImage, activeVideo, deletedImage, expiredVideo));
        ProfileStatusEligibilityClient eligibility = mock(ProfileStatusEligibilityClient.class);
        when(eligibility.evaluate("1", Collections.singleton("2")))
                .thenReturn(Collections.singletonMap("2", "ALLOW"));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        StatusFeedPage page = new StatusFeedService(repository,
                new StatusAccessAuthorizationService(eligibility, clock), clock).getFeed("1", null, 20);

        assertEquals(Arrays.asList(activeImage.getStatusId(), activeVideo.getStatusId()),
                page.getStatuses().stream().map(item -> item.getStatusId())
                        .collect(java.util.stream.Collectors.toList()));
    }

    @Test void mediaAccessReevaluatesDynamicEligibilityAndExpiresAtEquality() {
        StatusCatalogRecord active = record(StatusMediaType.IMAGE, NOW.minusSeconds(1), NOW.plusSeconds(10));
        StatusCatalogRecord equalityExpired = record(StatusMediaType.VIDEO, NOW.minusSeconds(2), NOW);
        StatusCatalogRepository repository = mock(StatusCatalogRepository.class);
        when(repository.findById(active.getStatusId())).thenReturn(Optional.of(active));
        when(repository.findById(equalityExpired.getStatusId())).thenReturn(Optional.of(equalityExpired));
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.exists(active.getMediaReference())).thenReturn(true);
        when(store.open(active.getMediaReference())).thenReturn(new ByteArrayInputStream(new byte[] { 1 }));
        AtomicReference<String> decision = new AtomicReference<>("DENY");
        ProfileStatusEligibilityClient eligibility = mock(ProfileStatusEligibilityClient.class);
        when(eligibility.evaluate("1", Collections.singleton("2")))
                .thenAnswer(invocation -> Collections.singletonMap("2", decision.get()));
        StatusMediaAccessService media = new StatusMediaAccessService(repository,
                new StatusAccessAuthorizationService(eligibility, Clock.fixed(NOW, ZoneOffset.UTC)), store);

        assertEquals(StatusAccessException.Category.NOT_FOUND,
                assertThrows(StatusAccessException.class, () -> media.open("1", active.getStatusId())).getCategory());
        decision.set("ALLOW");
        assertEquals(StatusMediaType.IMAGE, media.open("1", active.getStatusId()).getMediaType());
        decision.set("DENY");
        assertThrows(StatusAccessException.class, () -> media.open("1", active.getStatusId()));
        assertThrows(StatusAccessException.class, () -> media.open("1", equalityExpired.getStatusId()));
        verify(store, never()).open(equalityExpired.getMediaReference());
    }

    private StatusCatalogRecord record(StatusMediaType type, Instant created, Instant expires) {
        StatusCatalogRecord record = new StatusCatalogRecord("2", type, created, expires,
                UUID.randomUUID().toString(), IdempotencyProvenance.CLIENT);
        record.attachVerifiedMedia(UUID.randomUUID() + (type == StatusMediaType.VIDEO ? ".mp4" : ".jpg"),
                "a".repeat(64));
        record.transitionTo(StatusLifecycleState.MEDIA_READY);
        record.transitionTo(StatusLifecycleState.ACTIVE);
        return record;
    }
}
