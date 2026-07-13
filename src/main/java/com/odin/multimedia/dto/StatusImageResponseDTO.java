package com.odin.multimedia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import com.odin.multimedia.enums.StatusMediaType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusImageResponseDTO {
    private String statusId;
    private String catalogStatusId;
    private Instant createdAt;
    private Instant expiresAt;
    private StatusMediaType mediaType;
}
