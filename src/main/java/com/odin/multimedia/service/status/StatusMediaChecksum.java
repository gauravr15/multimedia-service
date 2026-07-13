package com.odin.multimedia.service.status;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class StatusMediaChecksum {

    private StatusMediaChecksum() {
    }

    public static String sha256(byte[] bytes) {
        if (bytes == null) throw new IllegalArgumentException("bytes are required");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
