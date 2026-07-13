package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.dto.StatusRefreshHint;

class StatusHintContractTest {
    @Test void canonicalHintUsesOpaqueStatusIdAndNoMediaReference() {
        StatusRefreshHint hint = StatusRefreshHint.builder().eventType("STATUS_CHANGED")
                .statusId("opaque-catalog-id").uploaderId("7").changeType("UPSERT")
                .occurredAt(Instant.EPOCH).traceId("trace").minimumCapability(StatusRefreshHint.CAPABILITY).build();
        assertEquals("opaque-catalog-id", hint.getStatusId());
        assertEquals("STATUS_QUERY_V1", hint.getMinimumCapability());
        assertArrayEquals(new String[] { "changeType", "eventType", "minimumCapability", "occurredAt",
                "statusId", "traceId", "uploaderId" },
                java.util.Arrays.stream(StatusRefreshHint.class.getDeclaredFields())
                        .filter(field -> !field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                        .map(java.lang.reflect.Field::getName).sorted().toArray(String[]::new));
    }
}
