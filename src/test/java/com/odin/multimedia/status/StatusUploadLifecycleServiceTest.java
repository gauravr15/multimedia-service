package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.odin.multimedia.config.StatusDurationProperties;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.IdempotencyProvenance;
import com.odin.multimedia.enums.StatusFailureReason;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.service.status.StatusCatalogPersistenceService;
import com.odin.multimedia.service.status.StatusMediaChecksum;
import com.odin.multimedia.service.status.StatusMediaCommitResult;
import com.odin.multimedia.service.status.StatusMediaStore;
import com.odin.multimedia.service.status.StatusMediaVerificationResult;
import com.odin.multimedia.service.status.StatusUploadLifecycleService;
import com.odin.multimedia.service.status.StatusUploadResult;

class StatusUploadLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-11T10:15:30Z");
    private StatusCatalogPersistenceService catalog;
    private StatusMediaStore mediaStore;
    private StatusUploadLifecycleService service;

    @BeforeEach
    void setUp() {
        catalog = mock(StatusCatalogPersistenceService.class);
        mediaStore = mock(StatusMediaStore.class);
        StatusDurationProperties duration = new StatusDurationProperties();
        duration.setDurationHours(24);
        duration.setMaxDurationHours(168);
        service = new StatusUploadLifecycleService(catalog, mediaStore, duration,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(catalog.findByIdempotencyKey(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void imageAndVideoFollowSameLifecycleAndRemainIndependent() {
        StatusUploadResult image = successfulUpload("image-key", StatusMediaType.IMAGE, bytes("image"));
        StatusUploadResult video = successfulUpload("video-key", StatusMediaType.VIDEO, bytes("video"));

        assertEquals(StatusLifecycleState.ACTIVE, image.getRecord().getLifecycleState());
        assertEquals(StatusLifecycleState.ACTIVE, video.getRecord().getLifecycleState());
        assertEquals(StatusMediaType.IMAGE, image.getRecord().getMediaType());
        assertEquals(StatusMediaType.VIDEO, video.getRecord().getMediaType());
        assertNotEquals(image.getRecord().getStatusId(), video.getRecord().getStatusId());
        assertEquals(NOW.plusSeconds(86400), image.getRecord().getExpiresAt());
        assertTrue(image.getRecord().getChecksum().matches("[0-9a-f]{64}"));
    }

    @Test
    void completedClientRetryReturnsSameStatusAndDifferentContentIsRejected() {
        byte[] content = bytes("same");
        StatusCatalogRecord active = activeRecord("customer", "client-key", StatusMediaType.IMAGE, content);
        when(catalog.findByIdempotencyKey("customer", "client-key")).thenReturn(Optional.of(active));

        StatusUploadResult duplicate = service.upload("customer", StatusMediaType.IMAGE, content, "client-key");

        assertFalse(duplicate.isNewlyActivated());
        assertEquals(active.getStatusId(), duplicate.getRecord().getStatusId());
        assertThrows(IllegalStateException.class,
                () -> service.upload("customer", StatusMediaType.IMAGE, bytes("different"), "client-key"));
        verify(mediaStore, never()).store(any());
    }

    @Test
    void missingClientKeyUsesLegacyGeneratedProvenance() {
        successfulUpload(null, StatusMediaType.IMAGE, bytes("image"));
        verify(catalog).createUploading(eq("customer"), eq(StatusMediaType.IMAGE), eq(NOW),
                eq(NOW.plusSeconds(86400)), any(), eq(IdempotencyProvenance.LEGACY_GENERATED));
    }

    @Test
    void storeAndVerificationFailuresNeverActivateAndAreCompensated() {
        StatusCatalogRecord uploading = uploading("customer", "store-fail", StatusMediaType.IMAGE);
        when(catalog.createUploading(any(), any(), any(), any(), any(), any())).thenReturn(uploading);
        when(mediaStore.store(any())).thenThrow(new IllegalStateException("store failed"));

        assertThrows(IllegalStateException.class,
                () -> service.upload("customer", StatusMediaType.IMAGE, bytes("image"), "store-fail"));
        verify(catalog).markFailed(uploading.getStatusId(), StatusFailureReason.MEDIA_STORE_FAILED);
        verify(catalog, never()).activate(any());

        setUp();
        uploading = uploading("customer", "verify-fail", StatusMediaType.VIDEO);
        when(catalog.createUploading(any(), any(), any(), any(), any(), any())).thenReturn(uploading);
        String checksum = StatusMediaChecksum.sha256(bytes("video"));
        when(mediaStore.store(any())).thenReturn(new StatusMediaCommitResult("media.mp4", checksum));
        when(mediaStore.verify("media.mp4", checksum)).thenReturn(new StatusMediaVerificationResult(true, false));

        assertThrows(IllegalStateException.class,
                () -> service.upload("customer", StatusMediaType.VIDEO, bytes("video"), "verify-fail"));
        verify(mediaStore).delete("media.mp4");
        verify(catalog).markFailed(uploading.getStatusId(), StatusFailureReason.MEDIA_VERIFICATION_FAILED);
        verify(catalog, never()).activate(any());
    }

    @Test
    void activationFailureDoesNotReturnSuccess() {
        byte[] content = bytes("image");
        StatusCatalogRecord record = prepareSuccessfulBoundaries("activation-fail", StatusMediaType.IMAGE, content);
        doThrow(new IllegalStateException("activation failed")).when(catalog).activate(record.getStatusId());

        assertThrows(IllegalStateException.class,
                () -> service.upload("customer", StatusMediaType.IMAGE, content, "activation-fail"));
        verify(catalog).markFailed(record.getStatusId(), StatusFailureReason.ACTIVATION_FAILED);
    }

    @Test
    void rejectsEmptyMediaBeforeCatalogOrStore() {
        assertThrows(IllegalArgumentException.class,
                () -> service.upload("customer", StatusMediaType.IMAGE, new byte[0], "key"));
        verify(catalog, never()).createUploading(any(), any(), any(), any(), any(), any());
        verify(mediaStore, never()).store(any());
    }

    private StatusUploadResult successfulUpload(String key, StatusMediaType type, byte[] content) {
        StatusCatalogRecord record = prepareSuccessfulBoundaries(key, type, content);
        return service.upload("customer", type, content, key);
    }

    private StatusCatalogRecord prepareSuccessfulBoundaries(String key, StatusMediaType type, byte[] content) {
        String effectiveKey = key == null ? "generated-placeholder" : key;
        StatusCatalogRecord record = uploading("customer", effectiveKey, type);
        when(catalog.createUploading(any(), any(), any(), any(), any(), any())).thenReturn(record);
        String checksum = StatusMediaChecksum.sha256(content);
        String reference = type == StatusMediaType.VIDEO ? "media.mp4" : "media.jpg";
        when(mediaStore.store(any())).thenReturn(new StatusMediaCommitResult(reference, checksum));
        when(mediaStore.verify(reference, checksum)).thenReturn(new StatusMediaVerificationResult(true, true));
        doAnswer(invocation -> {
            record.attachVerifiedMedia(reference, checksum);
            record.transitionTo(StatusLifecycleState.MEDIA_READY);
            return record;
        }).when(catalog).markMediaReady(record.getStatusId(), reference, checksum);
        doAnswer(invocation -> {
            record.transitionTo(StatusLifecycleState.ACTIVE);
            return record;
        }).when(catalog).activate(record.getStatusId());
        return record;
    }

    private StatusCatalogRecord activeRecord(String uploader, String key, StatusMediaType type, byte[] content) {
        StatusCatalogRecord record = uploading(uploader, key, type);
        record.attachVerifiedMedia("existing.media", StatusMediaChecksum.sha256(content));
        record.transitionTo(StatusLifecycleState.MEDIA_READY);
        record.transitionTo(StatusLifecycleState.ACTIVE);
        return record;
    }

    private StatusCatalogRecord uploading(String uploader, String key, StatusMediaType type) {
        return new StatusCatalogRecord(uploader, type, NOW, NOW.plusSeconds(86400), key,
                key.startsWith("generated") ? IdempotencyProvenance.LEGACY_GENERATED : IdempotencyProvenance.CLIENT);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
