package com.odin.multimedia.entity;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.odin.multimedia.enums.ImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table(name = "MW_IMAGE_DATA")
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "image_type")
	private ImageType imageType;
	
	@CreationTimestamp
	@Column(name = "upload_date")
	private Timestamp uploadDate;
	
	@Column(name = "sequence")
	private Long sequence;
	
	@Column(name = "image_path")
	private String imagePath;
	
	@Column(name = "is_active")
	private Boolean isActive;
	
	@Column(name = "is_deleted")
	private Boolean isDeleted;

}
