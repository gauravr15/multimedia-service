package com.odin.multimedia.service.status;

import java.io.InputStream;

public interface StatusMediaStore {
    StatusMediaCommitResult store(StatusMediaStoreRequest request);
    StatusMediaVerificationResult verify(String mediaReference, String checksum);
    boolean exists(String mediaReference);
    InputStream open(String mediaReference);
    void delete(String mediaReference);
    boolean isDurabilityQualified();
}
