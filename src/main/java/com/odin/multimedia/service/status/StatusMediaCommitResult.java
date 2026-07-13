package com.odin.multimedia.service.status;

public final class StatusMediaCommitResult {
    private final String mediaReference;
    private final String checksum;

    public StatusMediaCommitResult(String mediaReference, String checksum) {
        this.mediaReference = mediaReference;
        this.checksum = checksum;
    }

    public String getMediaReference() { return mediaReference; }
    public String getChecksum() { return checksum; }
}
