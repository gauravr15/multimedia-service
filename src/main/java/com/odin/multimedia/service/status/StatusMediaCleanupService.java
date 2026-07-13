package com.odin.multimedia.service.status;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusMediaCleanupService {
    private final StatusMediaStore mediaStore;
    public boolean cleanup(String mediaReference) {
        try {
            mediaStore.delete(mediaReference);
            return true;
        } catch (RuntimeException failure) {
            return false;
        }
    }
}
