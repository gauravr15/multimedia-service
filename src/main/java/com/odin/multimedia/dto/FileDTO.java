package com.odin.multimedia.dto;

import java.sql.Timestamp;

import com.odin.multimedia.enums.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
	
	private Long id;
	private String customerId;
	private ImageType fileType;
	private String fileName;
	private String filePath;
	private String fileExtension;
	private String fileMimeType;
	private Boolean isActive;
	private Boolean isDeleted;
	private Timestamp creationTimestamp;
	private Timestamp updateTimestamp;

}
