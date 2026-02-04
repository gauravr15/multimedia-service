package com.odin.multimedia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusImageDTO {
    private Long timestamp;
    private String uploaderCustomerId;
    private String fileData;
}
