package com.odin.multimedia.dto;

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
public class FileDataRSDTO {
	
	private String fileName;
	
	private String mimeType;
	
	private String fileData;
	
	private String fileExtension;

}
