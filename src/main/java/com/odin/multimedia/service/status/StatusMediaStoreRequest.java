package com.odin.multimedia.service.status;

import java.util.Arrays;

import com.odin.multimedia.enums.StatusMediaType;

public final class StatusMediaStoreRequest {
    private final byte[] content;
    private final StatusMediaType mediaType;

    public StatusMediaStoreRequest(byte[] content, StatusMediaType mediaType) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("content is required");
        if (mediaType == null) throw new IllegalArgumentException("mediaType is required");
        this.content = Arrays.copyOf(content, content.length);
        this.mediaType = mediaType;
    }

    public byte[] getContent() { return Arrays.copyOf(content, content.length); }
    public StatusMediaType getMediaType() { return mediaType; }
}
