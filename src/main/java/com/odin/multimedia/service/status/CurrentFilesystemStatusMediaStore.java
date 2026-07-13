package com.odin.multimedia.service.status;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.odin.multimedia.config.StatusMediaFilesystemProperties;
import com.odin.multimedia.enums.StatusMediaType;

/** Transitional local filesystem implementation; not production HA. */
@Component
public class CurrentFilesystemStatusMediaStore implements StatusMediaStore {

    private final Path root;

    public CurrentFilesystemStatusMediaStore(StatusMediaFilesystemProperties properties) {
        this.root = properties.rootPath();
    }

    @Override
    public StatusMediaCommitResult store(StatusMediaStoreRequest request) {
        try {
            Files.createDirectories(root);
            String name = UUID.randomUUID().toString() + extension(request.getMediaType());
            Path target = resolveOwned(name);
            Path temp = Files.createTempFile(root, ".status-", ".tmp");
            try {
                byte[] content = request.getContent();
                Files.write(temp, content, StandardOpenOption.TRUNCATE_EXISTING);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                return new StatusMediaCommitResult(name, StatusMediaChecksum.sha256(content));
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store status media", e);
        }
    }

    @Override
    public StatusMediaVerificationResult verify(String mediaReference, String checksum) {
        Path path = resolveOwned(mediaReference);
        if (!Files.isRegularFile(path)) return new StatusMediaVerificationResult(false, false);
        try {
            return new StatusMediaVerificationResult(true,
                    StatusMediaChecksum.sha256(Files.readAllBytes(path)).equalsIgnoreCase(checksum));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to verify status media", e);
        }
    }

    @Override
    public boolean exists(String mediaReference) {
        return Files.isRegularFile(resolveOwned(mediaReference));
    }

    @Override
    public InputStream open(String mediaReference) {
        try {
            Path path = resolveOwned(mediaReference);
            if (!Files.isRegularFile(path)) throw new IllegalStateException("Status media is unavailable");
            return Files.newInputStream(path, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open status media", e);
        }
    }

    @Override
    public void delete(String mediaReference) {
        try {
            Files.deleteIfExists(resolveOwned(mediaReference));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete status media", e);
        }
    }

    @Override
    public boolean isDurabilityQualified() {
        return false;
    }

    private Path resolveOwned(String reference) {
        if (reference == null || reference.trim().isEmpty()) throw new IllegalArgumentException("mediaReference is required");
        Path relative = java.nio.file.Paths.get(reference);
        if (relative.isAbsolute() || relative.getNameCount() != 1 || reference.contains("..")) {
            throw new IllegalArgumentException("Invalid media reference");
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.getParent().equals(root)) throw new IllegalArgumentException("Invalid media reference");
        return resolved;
    }

    private static String extension(StatusMediaType type) {
        return type == StatusMediaType.VIDEO ? ".mp4" : ".jpg";
    }

}
