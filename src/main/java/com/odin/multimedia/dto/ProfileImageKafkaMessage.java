package com.odin.multimedia.dto;

import java.time.Instant;

import com.odin.multimedia.enums.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageKafkaMessage {

    private String customerId;
    private Long imageId;
    private ImageType imageType;
    private String fileName;
    private String fileExtension;
    private String fileMimeType;
    private String filePath;
    private Instant uploadedAt;

    @Builder.Default
    private String source = "multimedia-service";
}
