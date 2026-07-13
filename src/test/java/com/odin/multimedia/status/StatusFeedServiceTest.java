package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.odin.multimedia.dto.StatusFeedPage;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;
import com.odin.multimedia.repository.StatusCatalogRepository;
import com.odin.multimedia.service.status.*;

class StatusFeedServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-11T12:00:00Z");
    private StatusCatalogRepository repository;
    private ProfileStatusEligibilityClient profile;
    private StatusFeedService feed;

    @BeforeEach void setUp() {
        repository = mock(StatusCatalogRepository.class);
        profile = mock(ProfileStatusEligibilityClient.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        feed = new StatusFeedService(repository, new StatusAccessAuthorizationService(profile, clock), clock);
    }

    @Test void returnsIndependentMixedStatusesAndBatchesUploadersOnce() {
        List<StatusCatalogRecord> records = Arrays.asList(active("2", StatusMediaType.IMAGE, NOW.minusSeconds(2)),
                active("2", StatusMediaType.VIDEO, NOW.minusSeconds(3)),
                active("3", StatusMediaType.IMAGE, NOW.minusSeconds(4)));
        when(repository.findFeedCandidates(any(), any(), isNull(), isNull(), any())).thenReturn(records);
        when(profile.evaluate(eq("1"), any())).thenReturn(map("2", "ALLOW", "3", "DENY"));
        StatusFeedPage page = feed.getFeed("1", null, 20);
        assertEquals(2, page.getStatuses().size());
        assertEquals(StatusMediaType.IMAGE, page.getStatuses().get(0).getMediaType());
        assertEquals(StatusMediaType.VIDEO, page.getStatuses().get(1).getMediaType());
        assertNull(page.getNextCursor());
        verify(profile, times(1)).evaluate(eq("1"), eq(new LinkedHashSet<>(Arrays.asList("2", "3"))));
    }

    @Test void excludesSelfInvalidAndExpiredBeforeAuthorization() {
        StatusCatalogRecord self = active("1", StatusMediaType.IMAGE, NOW.minusSeconds(1));
        StatusCatalogRecord uploading = new StatusCatalogRecord("2", StatusMediaType.IMAGE, NOW, NOW.plusSeconds(60),
                "u", IdempotencyProvenance.CLIENT);
        when(repository.findFeedCandidates(any(), any(), any(), any(), any())).thenReturn(Arrays.asList(self, uploading));
        when(profile.evaluate(eq("1"), eq(Collections.emptySet()))).thenReturn(Collections.emptyMap());
        assertTrue(feed.getFeed("1", null, null).getStatuses().isEmpty());
    }

    @Test void indeterminateAndMalformedCursorFailRetryablyOrSafely() {
        StatusCatalogRecord record = active("2", StatusMediaType.IMAGE, NOW.minusSeconds(1));
        when(repository.findFeedCandidates(any(), any(), any(), any(), any())).thenReturn(Collections.singletonList(record));
        when(profile.evaluate(anyString(), any())).thenReturn(Collections.singletonMap("2", "INDETERMINATE"));
        assertEquals(StatusAccessException.Category.DEPENDENCY_FAILURE,
                assertThrows(StatusAccessException.class, () -> feed.getFeed("1", null, 20)).getCategory());
        assertEquals(StatusAccessException.Category.INVALID_REQUEST,
                assertThrows(StatusAccessException.class, () -> feed.getFeed("1", "bad", 20)).getCategory());
        assertThrows(StatusAccessException.class, () -> feed.getFeed("1", null, 51));
    }

	@Test void repairRequiredViewerFailsBeforeCatalogQueryAndReadyEmptyIsLegitimate() {
		doThrow(new StatusAccessException(StatusAccessException.Category.REPAIR_REQUIRED, "repair"))
				.when(profile).requireReady("1");
		assertEquals(StatusAccessException.Category.REPAIR_REQUIRED,
				assertThrows(StatusAccessException.class, () -> feed.getFeed("1", null, 20)).getCategory());
		verifyNoInteractions(repository);
		reset(profile, repository);
		when(repository.findFeedCandidates(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
		when(profile.evaluate("1", Collections.emptySet())).thenReturn(Collections.emptyMap());
		assertTrue(feed.getFeed("1", null, 20).getStatuses().isEmpty());
	}

    private StatusCatalogRecord active(String uploader, StatusMediaType type, Instant created) {
        StatusCatalogRecord record = new StatusCatalogRecord(uploader, type, created, NOW.plusSeconds(3600),
                UUID.randomUUID().toString(), IdempotencyProvenance.CLIENT);
        record.attachVerifiedMedia(UUID.randomUUID() + (type == StatusMediaType.VIDEO ? ".mp4" : ".jpg"),
                StatusMediaChecksum.sha256((uploader + type + created).getBytes(StandardCharsets.UTF_8)));
        record.transitionTo(StatusLifecycleState.MEDIA_READY); record.transitionTo(StatusLifecycleState.ACTIVE);
        return record;
    }
    private Map<String,String> map(String... values) { Map<String,String> m=new HashMap<>(); for(int i=0;i<values.length;i+=2)m.put(values[i],values[i+1]); return m; }
}
