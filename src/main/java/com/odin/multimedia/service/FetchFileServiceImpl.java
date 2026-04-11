package com.odin.multimedia.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.FetchImageDTO;
import com.odin.multimedia.dto.FileDTO;
import com.odin.multimedia.dto.FileDataRSDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.dto.StatusImageDTO;
import com.odin.multimedia.dto.StatusMediaDTO;
import com.odin.multimedia.enums.CustomerType;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.repository.FileRepository;
import com.odin.multimedia.utility.ResponseObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FetchFileServiceImpl implements FetchFileService{
	
	@Autowired
	private ResponseObject responseObj;
	
	@Autowired
	private FileRepository fileRepo;

	@Autowired
	private StatusImageService statusImageService;

	@Override
	public ResponseDTO fetchFile(HttpServletRequest request, FetchImageDTO fetchImageDTO) {
		String userType = request.getHeader(ApplicationConstants.USER_TYPE);
		if(userType.equals(CustomerType.CUSTOMER.name())) {
			return fetchCustomerFile(request, fetchImageDTO);
		}
		else {
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.FAILURE_CODE);
		}
	}

	private ResponseDTO fetchCustomerFile(HttpServletRequest request, FetchImageDTO fetchImageDTO) {
		if(!ObjectUtils.isEmpty(fetchImageDTO.getImageType()) && fetchImageDTO.getImageType() == ImageType.PROFILE_IMG) {
			return fetchCustomerProfilePhoto(request, fetchImageDTO);
		} else if (!ObjectUtils.isEmpty(fetchImageDTO.getImageType()) && fetchImageDTO.getImageType() == ImageType.STATUS_IMG) {
			if (!ObjectUtils.isEmpty(fetchImageDTO.getStatusKey())) {
				return fetchSpecificStatusImage(fetchImageDTO);
			}
			return fetchCustomerStatusMedia(request);
		} else {
			return fetchFileById(request, fetchImageDTO);
		}
	}

	private ResponseDTO fetchSpecificStatusImage(FetchImageDTO fetchImageDTO) {
		try {
			String statusKey = fetchImageDTO.getStatusKey();
			String filePath = statusImageService.getFilePathByKey(statusKey);

			if (filePath == null) {
				log.warn("Status image not found or expired for key: {}", statusKey);
				return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
			}

			File file = new File(filePath);
			if (!file.exists()) {
				log.warn("Status file missing from disk: {}", filePath);
				return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
			}

			String fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

			// Parse timestamp from key (customerId:STATUS_IMG:timestamp)
			long timestamp = 0;
			try {
				int lastIndex = statusKey.lastIndexOf(":");
				timestamp = Long.parseLong(statusKey.substring(lastIndex + 1));
			} catch (Exception e) {
				log.warn("Could not parse timestamp from statusKey: {}", statusKey);
			}

			StatusImageDTO response = StatusImageDTO.builder()
					.timestamp(timestamp)
					.uploaderCustomerId(fetchImageDTO.getUploaderCustomerId())
					.fileData(fileContent)
					.build();

			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, response);
		} catch (Exception e) {
			log.error("Error occurred while fetching specific status image", e);
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
		}
	}

	private ResponseDTO fetchCustomerStatusMedia(HttpServletRequest request) {
		try {
			String customerId = request.getHeader(ApplicationConstants.CUSTOMER_ID);
			if (customerId == null || customerId.isEmpty()) {
				return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.FAILURE_CODE);
			}

			java.util.List<StatusMediaDTO> media = statusImageService.fetchStatusMedia(customerId);
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, media);
		} catch (Exception e) {
			log.error("Error occurred while fetching status media", e);
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
		}
	}

	private ResponseDTO fetchFileById(HttpServletRequest request, FetchImageDTO fetchImageDTO) {
		try {
			FileDTO fileDTO = fileRepo.findByfileTypeAndImageIdAndIsActive(fetchImageDTO.getImageType(),
					fetchImageDTO.getImageId(), true);
			File file = new File(fileDTO.getFilePath());
			if (!file.exists()) {
				return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
			}
			File fileData = new File(fileDTO.getFilePath());
			String fileContent = new String(Files.readAllBytes(fileData.toPath()), StandardCharsets.UTF_8);
			FileDataRSDTO response = FileDataRSDTO.builder().fileExtension(fileDTO.getFileExtension())
					.fileName(fileDTO.getFileName()).fileData(fileContent).mimeType(fileDTO.getFileMimeType()).build();

			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, response);
		} catch (Exception e) {
			log.error("Error occured while fetching profile photo : {}", ExceptionUtils.getStackTrace(e));
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
		}
	}

	private ResponseDTO fetchCustomerProfilePhoto(HttpServletRequest request, FetchImageDTO fetchImageDTO) {
		try {
		// Support peer photo fetch: if targetCustomerId is provided, fetch that user's photo
		String targetId = fetchImageDTO.getTargetCustomerId();
                String headerCustomerId = request.getHeader(ApplicationConstants.CUSTOMER_ID);
                String customerId = (targetId != null && !targetId.trim().isEmpty())
                                ? targetId
                                : headerCustomerId;

                log.info("[FETCH-PHOTO] Resolved customerId={} | targetId={} | headerCustomerId={}",
                                customerId, targetId, headerCustomerId);

                List<FileDTO> fileListDTO = fileRepo.findByfileTypeAndCustomerIdAndIsActive(fetchImageDTO.getImageType().name(), customerId, true);
                log.info("[FETCH-PHOTO] fileRepo returned {} FileDTOs for customerId={}",
                                fileListDTO == null ? "null" : fileListDTO.size(), customerId);

                FileDTO fileDTO = null;
                if (fileListDTO != null && !fileListDTO.isEmpty()) {
                        fileDTO = fileListDTO.get(0);
                        log.info("[FETCH-PHOTO] Selected FileDTO: id={} customerId={} fileType={} isActive={}",
                                        fileDTO.getId(), fileDTO.getCustomerId(), fileDTO.getFileType(), fileDTO.getIsActive());
                        log.info("[FETCH-PHOTO] File path from DB: {}", fileDTO.getFilePath());
                }
                if (fileDTO == null || fileDTO.getFilePath() == null) {
                        log.warn("[FETCH-PHOTO] NO_DATA_FOUND: fileDTO={} for customerId={}",
                                        fileDTO == null ? "null" : "filePath-null", customerId);
                        return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
        }

        // Read the file from the system
        File file = new File(fileDTO.getFilePath());
        log.info("[FETCH-PHOTO] Checking file existence: path={} exists={}", file.getAbsolutePath(), file.exists());
        if (!file.exists()) {
                log.warn("[FETCH-PHOTO] FILE NOT ON DISK for customerId={} path={}", customerId, fileDTO.getFilePath());
                return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
        }
        File fileData = new File(fileDTO.getFilePath());
        String fileContent = new String(Files.readAllBytes(fileData.toPath()), StandardCharsets.UTF_8);
        log.info("[FETCH-PHOTO] File read OK for customerId={} size={} bytes", customerId, fileContent.length());
                FileDataRSDTO response = FileDataRSDTO.builder().fileExtension(fileDTO.getFileExtension())
                                .fileName(fileDTO.getFileName()).fileData(fileContent).mimeType(fileDTO.getFileMimeType()).build();

        return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, response);

                }catch(Exception e) {
                        log.error("[FETCH-PHOTO] Exception for customerId: {}", ExceptionUtils.getStackTrace(e));
                        return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
                }
        }
}
