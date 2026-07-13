package com.odin.multimedia.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatusRefreshHint {
    public static final String CAPABILITY = "STATUS_QUERY_V1";

    String eventType;
    String statusId;
    String uploaderId;
    String changeType;
    Instant occurredAt;
    String traceId;
    String minimumCapability;
}
