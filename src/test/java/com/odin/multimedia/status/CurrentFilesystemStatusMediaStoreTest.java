package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.odin.multimedia.config.StatusMediaFilesystemProperties;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.service.status.*;

class CurrentFilesystemStatusMediaStoreTest {
    @TempDir Path root;
    @Test void storesVerifiesAndDeletesBothTypes() {
        CurrentFilesystemStatusMediaStore store = store();
        StatusMediaCommitResult image = store.store(req("image", StatusMediaType.IMAGE));
        StatusMediaCommitResult video = store.store(req("video", StatusMediaType.VIDEO));
        assertTrue(image.getMediaReference().endsWith(".jpg"));
        assertTrue(video.getMediaReference().endsWith(".mp4"));
        assertTrue(store.verify(image.getMediaReference(), image.getChecksum()).isValid());
        assertFalse(store.isDurabilityQualified());
        store.delete(image.getMediaReference()); store.delete(image.getMediaReference());
        assertFalse(store.exists(image.getMediaReference()));
    }
    @Test void rejectsTraversalAndProtectsSibling() throws Exception {
        CurrentFilesystemStatusMediaStore store = store();
        Path sibling = root.getParent().resolve("sibling-status-file");
        Files.write(sibling, "safe".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> store.delete("../sibling-status-file"));
        assertTrue(Files.exists(sibling)); Files.deleteIfExists(sibling);
    }
    @Test void reportsMissingAndChecksumMismatch() {
        CurrentFilesystemStatusMediaStore store = store();
        assertFalse(store.verify("missing.jpg", "a".repeat(64)).exists());
        StatusMediaCommitResult result = store.store(req("image", StatusMediaType.IMAGE));
        assertFalse(store.verify(result.getMediaReference(), "b".repeat(64)).checksumMatches());
    }
    private CurrentFilesystemStatusMediaStore store() {
        StatusMediaFilesystemProperties p = new StatusMediaFilesystemProperties(); p.setRoot(root.toString());
        return new CurrentFilesystemStatusMediaStore(p);
    }
    private StatusMediaStoreRequest req(String value, StatusMediaType type) {
        return new StatusMediaStoreRequest(value.getBytes(StandardCharsets.UTF_8), type);
    }
}
