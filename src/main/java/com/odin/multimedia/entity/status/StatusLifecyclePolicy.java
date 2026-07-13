package com.odin.multimedia.entity.status;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.odin.multimedia.enums.StatusLifecycleState;

public final class StatusLifecyclePolicy {

    private static final Map<StatusLifecycleState, Set<StatusLifecycleState>> ALLOWED = new HashMap<>();

    static {
        ALLOWED.put(StatusLifecycleState.UPLOADING,
                EnumSet.of(StatusLifecycleState.MEDIA_READY, StatusLifecycleState.FAILED));
        ALLOWED.put(StatusLifecycleState.MEDIA_READY,
                EnumSet.of(StatusLifecycleState.ACTIVE, StatusLifecycleState.FAILED));
        ALLOWED.put(StatusLifecycleState.ACTIVE,
                EnumSet.of(StatusLifecycleState.DELETING, StatusLifecycleState.EXPIRED));
        ALLOWED.put(StatusLifecycleState.DELETING,
                EnumSet.of(StatusLifecycleState.DELETED, StatusLifecycleState.ACTIVE));
    }

    private StatusLifecyclePolicy() {
    }

    public static void validateTransition(StatusCatalogRecord record, StatusLifecycleState target) {
        if (record == null || target == null) throw new IllegalArgumentException("record and target are required");
        StatusLifecycleState current = record.getLifecycleState();
        Set<StatusLifecycleState> targets = ALLOWED.get(current);
        if (targets == null || !targets.contains(target)) {
            throw new IllegalStateException("Illegal status transition: " + current + " -> " + target);
        }
        if (target == StatusLifecycleState.ACTIVE) record.validateActivation();
    }
}
