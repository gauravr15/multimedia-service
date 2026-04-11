package com.odin.multimedia.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.FileDTO;
import com.odin.multimedia.dto.FileResponseDTO;
import com.odin.multimedia.dto.NotificationDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.dto.StatusImageResponseDTO;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.enums.NotificationChannel;
import com.odin.multimedia.repository.FileRepository;
import com.odin.multimedia.repository.ProfileRepository;
import com.odin.multimedia.utility.ResponseObject;
import com.odin.multimedia.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerFileUploadService implements FileUploadService {

	@Value("${allowed.file.extensions}")
	private List<String> ALLOWED_EXTENSIONS;
	
	@Value("${allowed.file.mime.type}")
	private List<String> ALLOWED_MIME_TYPES;
	
	@Value("${file.upload.base.dir}")
	private String BASE_DIR;

	@Value("${profile.service.url}")
	private String profileServiceUrl;

	@Autowired
	ResponseObject responseObj;

	@Autowired
	private FileRepository fileRepository;

	@Autowired
	private ProfileImageEventPublisher profileImageEventPublisher;

	@Autowired
	private StatusUpdateEventPublisher statusUpdateEventPublisher;

	@Autowired
	private StatusImageService statusImageService;
	
	@Autowired
	private ProfileRepository profileRepo;

	@Autowired
	private Utility utility;
	
	private final String PROFILE_STATUS_ENDPOINT = "status/notify-upload";

	@Override
	public ResponseDTO uploadFile(MultipartHttpServletRequest request, HttpServletResponse response, Map<String, String> headers, String imageType) {
		try {
			log.info("[UPLOAD-START] BASE_DIR={} for imageType={}", BASE_DIR, imageType);
			String customerId = headers.get(ApplicationConstants.CUSTOMER_ID.toLowerCase());
			if (customerId == null || customerId.isEmpty()) {
				throw new IllegalArgumentException("Missing customerId in request headers");
			}
			MultipartFile file = request.getFile("file");
			if (file == null) {
				throw new IllegalArgumentException("No file provided in the request");
			}

			ImageType resolvedImageType;
			try {
				resolvedImageType = ImageType.valueOf(imageType);
			} catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Invalid file type", ex);
			}

			if (resolvedImageType == ImageType.PROFILE_IMG) {
				Boolean profileImgUpdate = updateExistingProfileImage(customerId);
				if (!profileImgUpdate) {
					log.error("Failed to deactivate current profile image for customer id : {}", customerId);
					return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.FAILURE_CODE);
				}
			}

			String fileName = file.getOriginalFilename();
			if (fileName == null || fileName.isEmpty()) {
				throw new IllegalArgumentException("Invalid file name");
			}

			String fileExtension = getFileExtension(fileName);
			if (!ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
				throw new IllegalArgumentException("Unsupported file extension");
			}

			String mimeType = file.getContentType();
			if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
				throw new IllegalArgumentException("Unsupported MIME type");
			}

			byte[] fileBytes = file.getBytes();
			String base64EncodedContent = Base64.getEncoder().encodeToString(fileBytes);

			String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
	        File dateFolder = new File(BASE_DIR + File.separator + currentDate);
	        if (!dateFolder.exists()) {
	            dateFolder.mkdirs();
	        }

	        String uniqueFileName = UUID.randomUUID() + "." + ApplicationConstants.TXT;
	        File outputFile = new File(dateFolder, uniqueFileName);
	        log.info("[UPLOAD-FILE] Output file absolute path: {}", outputFile.getAbsolutePath());

	        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
	            fos.write(base64EncodedContent.getBytes());
	        }

			if (resolvedImageType == ImageType.STATUS_IMG) {
				String statusKey = statusImageService.storeStatusImage(customerId, outputFile.getAbsolutePath());
				
				// Publish status update notifications to Kafka for allowed customer IDs
				publishStatusUpdateNotifications(customerId, statusKey, headers);

				StatusImageResponseDTO responsePayload = StatusImageResponseDTO.builder().statusId(statusKey).build();
				return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, responsePayload);
			}

			String downscaledBase64 = null;
			if (resolvedImageType == ImageType.PROFILE_IMG) {
				downscaledBase64 = downscaleToBase64720p(fileBytes, fileExtension);
			}

			FileDTO metadata = new FileDTO();
			metadata.setCustomerId(customerId);
			metadata.setFileName(uniqueFileName);
			metadata.setFilePath(outputFile.getAbsolutePath());
			metadata.setFileType(resolvedImageType);
			metadata.setFileMimeType(mimeType);
			metadata.setFileExtension(fileExtension);
			metadata.setIsActive(true);
			metadata.setIsDeleted(false);
			log.info("[UPLOAD-METADATA] Saving metadata with filePath: {} for customerId: {}", metadata.getFilePath(), metadata.getCustomerId());

			FileDTO fileResponse = fileRepository.save(metadata);
			publishProfileImageEventIfNeeded(customerId, resolvedImageType, fileResponse, downscaledBase64);

			FileResponseDTO fileResponseDTO = FileResponseDTO.builder().imageId(fileResponse.getId()).build();
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, fileResponseDTO);

		} catch (Exception e) {
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
		}
	}

	private void publishStatusUpdateNotifications(String uploaderCustomerId, String statusKey, Map<String, String> headers) {
		log.info("Fetching allowed customer IDs for status update notification. uploaderCustomerId={}", uploaderCustomerId);
		try {
			// 1. Call Profile Service to get allowed customer IDs
			java.util.Map<String, String> requestBody = new java.util.HashMap<>();
			requestBody.put("customerId", uploaderCustomerId);

			ResponseDTO response = utility.makeRestCall(profileServiceUrl.concat(PROFILE_STATUS_ENDPOINT), requestBody, HttpMethod.POST, ResponseDTO.class);
			
			if (response != null && ResponseCodes.SUCCESS_CODE.equals(response.getStatusCode()) && response.getData() != null) {
				List<Long> allowedCustomerIds = utility.getInstances(response, Long.class);
				log.info("Found {} allowed customers for status update. uploaderCustomerId={}", 
						allowedCustomerIds.size(), uploaderCustomerId);
				
				String senderMobile = profileRepo.findByCustomerId(uploaderCustomerId).getMobile();
				log.info("gaurav uploader mobile : {}", senderMobile);
				
				

				for (Long allowedId : allowedCustomerIds) {
					java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
					notificationMap.put("files", Collections.singletonList(statusKey));
					notificationMap.put("senderCustomerId", uploaderCustomerId);
					notificationMap.put("senderMobile", senderMobile);
					notificationMap.put("senderPhone", senderMobile);

					NotificationDTO notification = NotificationDTO.builder()
							.customerId(allowedId)
							.notificationId(1L) // As per requirement
							.channel(NotificationChannel.INAPP)
							.map(notificationMap)
							.build();

					statusUpdateEventPublisher.publish(notification, String.valueOf(allowedId));
				}
			} else {
				log.warn("Profile service returned non-success or empty data for uploaderCustomerId={}. Response: {}", 
						uploaderCustomerId, response);
			}
		} catch (Exception ex) {
			log.error("Failed to publish status update notifications for uploaderCustomerId={}", uploaderCustomerId, ex);
		}
	}

	private static final String PROFILE_PHOTO_NOTIFY_ENDPOINT = "profiles/photo/notify-upload";
	private static final String PROFILE_PHOTO_VERSION_ENDPOINT = "profiles/photo-version/increment";

	private void publishProfileImageEventIfNeeded(String customerId, ImageType imageType, FileDTO fileResponse, String downscaledBase64) {
		try {
			if (imageType != ImageType.PROFILE_IMG || fileResponse == null || fileResponse.getId() == null) {
				return;
			}

			// 1. Increment photo version in profile-service
			incrementPhotoVersion(customerId);

			// 2. Get allowed viewer IDs from profile-service and publish notifications
			publishProfilePhotoNotifications(customerId, fileResponse);

			if (downscaledBase64 == null) {
				log.warn("Skipping profile image Kafka publish due to missing downscaled content for customerId={}", customerId);
				return;
			}

			java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
			notificationMap.put("profileImage", downscaledBase64);

			NotificationDTO notification = NotificationDTO.builder()
					.customerId(parseCustomerId(customerId))
					.notificationId(fileResponse.getId())
					.channel(NotificationChannel.INAPP)
					.map(notificationMap)
					.build();

			profileImageEventPublisher.publish(notification, customerId);
		} catch (Exception ex) {
			log.error("Failed to publish profile image event for customerId={}, imageId={}", customerId,
					fileResponse != null ? fileResponse.getId() : null, ex);
		}
	}

	private void incrementPhotoVersion(String customerId) {
		try {
			java.util.Map<String, String> requestBody = new java.util.HashMap<>();
			requestBody.put("customerId", customerId);

			ResponseDTO response = utility.makeRestCall(
					profileServiceUrl.concat(PROFILE_PHOTO_VERSION_ENDPOINT),
					requestBody, org.springframework.http.HttpMethod.POST, ResponseDTO.class);

			if (response != null && ResponseCodes.SUCCESS_CODE.equals(response.getStatusCode())) {
				log.info("Photo version incremented for customerId={}", customerId);
			} else {
				log.warn("Failed to increment photo version for customerId={}. Response: {}", customerId, response);
			}
		} catch (Exception ex) {
			log.error("Error incrementing photo version for customerId={}", customerId, ex);
		}
	}

	private void publishProfilePhotoNotifications(String customerId, FileDTO fileResponse) {
		try {
			java.util.Map<String, String> requestBody = new java.util.HashMap<>();
			requestBody.put("customerId", customerId);

			ResponseDTO response = utility.makeRestCall(
					profileServiceUrl.concat(PROFILE_PHOTO_NOTIFY_ENDPOINT),
					requestBody, org.springframework.http.HttpMethod.POST, ResponseDTO.class);

			if (response != null && ResponseCodes.SUCCESS_CODE.equals(response.getStatusCode()) && response.getData() != null) {
				List<Long> allowedCustomerIds = utility.getInstances(response, Long.class);
				log.info("Found {} allowed customers for profile photo update. customerId={}", 
						allowedCustomerIds.size(), customerId);

				String senderMobile = profileRepo.findByCustomerId(customerId).getMobile();

				for (Long allowedId : allowedCustomerIds) {
					java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
					notificationMap.put("senderCustomerId", customerId);
					notificationMap.put("senderMobile", senderMobile);
					notificationMap.put("profilePhotoUpdate", "true");

					NotificationDTO notification = NotificationDTO.builder()
							.customerId(allowedId)
							.notificationId(1L)
							.channel(NotificationChannel.INAPP)
							.map(notificationMap)
							.build();

					profileImageEventPublisher.publish(notification, String.valueOf(allowedId));
				}
			} else {
				log.warn("Profile service returned non-success for photo notify. customerId={}. Response: {}", 
						customerId, response);
			}
		} catch (Exception ex) {
			log.error("Failed to publish profile photo notifications for customerId={}", customerId, ex);
		}
	}

	private Long parseCustomerId(String customerId) {
		try {
			return Long.valueOf(customerId);
		} catch (NumberFormatException ex) {
			log.warn("Unable to parse customerId '{}' to Long; sending null", customerId);
			return null;
		}
	}

	private String downscaleToBase64720p(byte[] imageBytes, String fileExtension) {
		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
			if (source == null) {
				log.warn("Unable to read image bytes for downscale");
				return null;
			}

			int maxWidth = 1280;
			int maxHeight = 720;
			double widthScale = (double) maxWidth / source.getWidth();
			double heightScale = (double) maxHeight / source.getHeight();
			double scale = Math.min(1.0, Math.min(widthScale, heightScale));

			int targetWidth = (int) Math.round(source.getWidth() * scale);
			int targetHeight = (int) Math.round(source.getHeight() * scale);

			BufferedImage resized = new BufferedImage(targetWidth, targetHeight,
					source.getTransparency() == BufferedImage.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = resized.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
			graphics.dispose();

			String format = normalizeImageFormat(fileExtension);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				ImageIO.write(resized, format, baos);
				return Base64.getEncoder().encodeToString(baos.toByteArray());
			}
		} catch (Exception ex) {
			log.error("Failed to downscale profile image", ex);
			return null;
		}
	}

	private String normalizeImageFormat(String fileExtension) {
		if (fileExtension == null) {
			return "jpg";
		}
		String ext = fileExtension.toLowerCase();
		switch (ext) {
		case "jpeg":
		case "jpg":
			return "jpg";
		case "png":
			return "png";
		case "gif":
			return "gif";
		default:
			return "jpg";
		}
	}

	private Boolean updateExistingProfileImage(String customerId) {
		Boolean flag = false;
		List<FileDTO> fileResponseList = fileRepository.findByfileTypeAndCustomerIdAndIsActive(ImageType.PROFILE_IMG.name(), customerId, true);
		if(ObjectUtils.isEmpty(fileResponseList)) {
			flag = true;
		}
		else {
			FileDTO fileResponse = fileResponseList.get(0);
			fileResponse.setIsActive(false);
			fileResponse = fileRepository.update(fileResponse);
			if(ObjectUtils.isEmpty(fileResponse)) {
				flag = false;
			}
			else {
				flag = true;
			}
		}
		return flag;
	}

	private String getFileExtension(String fileName) {
		return fileName.substring(fileName.lastIndexOf('.') + 1);
	}

}
