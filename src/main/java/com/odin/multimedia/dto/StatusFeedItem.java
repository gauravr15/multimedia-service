package com.odin.multimedia.dto;

import java.time.Instant;
import com.odin.multimedia.enums.StatusMediaType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatusFeedItem {
    private final String statusId;
    private final String uploaderId;
    private final StatusMediaType mediaType;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean mediaAvailable;
    private final String mediaUrl;
}
