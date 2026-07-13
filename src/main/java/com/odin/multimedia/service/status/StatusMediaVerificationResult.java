package com.odin.multimedia.service.status;

public final class StatusMediaVerificationResult {
    private final boolean exists;
    private final boolean checksumMatches;

    public StatusMediaVerificationResult(boolean exists, boolean checksumMatches) {
        this.exists = exists;
        this.checksumMatches = checksumMatches;
    }

    public boolean exists() { return exists; }
    public boolean checksumMatches() { return checksumMatches; }
    public boolean isValid() { return exists && checksumMatches; }
}
