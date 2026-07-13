package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.status.*;

class StatusMediaAccessServiceTest {
    @Test void reauthorizesAndOpensThroughStoreWhileDenyDoesNotOpen() {
        Instant now=Instant.parse("2026-07-11T12:00:00Z");
        StatusCatalogRecord record=new StatusCatalogRecord("2",StatusMediaType.VIDEO,now.minusSeconds(1),now.plusSeconds(60),"k",IdempotencyProvenance.CLIENT);
        record.attachVerifiedMedia("safe.mp4","a".repeat(64)); record.transitionTo(StatusLifecycleState.MEDIA_READY); record.transitionTo(StatusLifecycleState.ACTIVE);
        StatusCatalogRepository repo=mock(StatusCatalogRepository.class); StatusMediaStore store=mock(StatusMediaStore.class);
        ProfileStatusEligibilityClient profile=mock(ProfileStatusEligibilityClient.class);
        when(repo.findById(record.getStatusId())).thenReturn(Optional.of(record)); when(store.exists("safe.mp4")).thenReturn(true);
        when(store.open("safe.mp4")).thenReturn(new ByteArrayInputStream(new byte[]{1}));
        when(profile.evaluate("1",Collections.singleton("2"))).thenReturn(Collections.singletonMap("2","ALLOW"));
        StatusMediaAccessService service=new StatusMediaAccessService(repo,new StatusAccessAuthorizationService(profile,Clock.fixed(now,ZoneOffset.UTC)),store);
        assertEquals(StatusMediaType.VIDEO,service.open("1",record.getStatusId()).getMediaType()); verify(store).open("safe.mp4");
        when(profile.evaluate("1",Collections.singleton("2"))).thenReturn(Collections.singletonMap("2","DENY"));
        assertEquals(StatusAccessException.Category.NOT_FOUND,assertThrows(StatusAccessException.class,()->service.open("1",record.getStatusId())).getCategory());
    }
}
