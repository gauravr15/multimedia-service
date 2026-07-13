package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.*;

class StatusLifecyclePolicyTest {
    @Test void allowsFoundationTransitions() {
        StatusCatalogRecord r = record();
        r.attachVerifiedMedia("opaque.jpg", "a".repeat(64));
        r.transitionTo(StatusLifecycleState.MEDIA_READY);
        r.transitionTo(StatusLifecycleState.ACTIVE);
        assertEquals(StatusLifecycleState.ACTIVE, r.getLifecycleState());
    }
    @Test void allowsFailureAndRejectsIllegalActivation() {
        StatusCatalogRecord failed = record();
        failed.markFailed(StatusFailureReason.MEDIA_STORE_FAILED);
        assertEquals(StatusLifecycleState.FAILED, failed.getLifecycleState());
        assertThrows(IllegalStateException.class, () -> failed.transitionTo(StatusLifecycleState.ACTIVE));
        assertThrows(IllegalStateException.class, () -> record().transitionTo(StatusLifecycleState.ACTIVE));
    }
    @Test void activeRequiresMediaAndChecksum() {
        StatusCatalogRecord r = record();
        r.transitionTo(StatusLifecycleState.MEDIA_READY);
        assertThrows(IllegalStateException.class, () -> r.transitionTo(StatusLifecycleState.ACTIVE));
        assertThrows(IllegalStateException.class, () -> r.attachVerifiedMedia("x", "bad"));
    }
    @Test void expiryMustFollowCreation() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThrows(IllegalStateException.class, () -> new StatusCatalogRecord("1", StatusMediaType.IMAGE,
                now, now, "key", IdempotencyProvenance.CLIENT));
    }
    private StatusCatalogRecord record() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new StatusCatalogRecord("89", StatusMediaType.IMAGE, now, now.plusSeconds(86400),
                "key-" + System.nanoTime(), IdempotencyProvenance.CLIENT);
    }
}
