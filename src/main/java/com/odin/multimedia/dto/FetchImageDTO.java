package com.odin.multimedia.dto;

import java.util.List;

import com.odin.multimedia.enums.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchImageDTO {

	private ImageType imageType;
	
	private List<Long> imageIds;
	
	private Long imageId;

	private String uploaderCustomerId;

	private String targetCustomerId;

	private String statusKey;
}
