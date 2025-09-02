package com.odin.multimedia.dto;

import java.sql.Timestamp;

import com.odin.multimedia.enums.ImageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageDTO {
	
	private Long id;
	private ImageType imageType;
	private Timestamp createTimestamp;
	private String customerId;
}
