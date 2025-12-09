package com.odin.multimedia.service;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.FileDTO;
import com.odin.multimedia.dto.FileResponseDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.repository.FileRepository;
import com.odin.multimedia.utility.ResponseObject;

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

	@Autowired
	ResponseObject responseObj;

	@Autowired
	private FileRepository fileRepository;

	@Override
	public ResponseDTO uploadFile(MultipartHttpServletRequest request, HttpServletResponse response, Map<String, String> headers, String imageType) {
		try {
			// Extract file and additional details from the request
			String customerId = headers.get(ApplicationConstants.CUSTOMER_ID.toLowerCase()).toString();
			MultipartFile file = request.getFile("file");
			if (file == null) {
				throw new IllegalArgumentException("No file provided in the request"); //--> getting in this block
			}
			if(ImageType.valueOf(imageType) == null) {
				throw new IllegalArgumentException("Invalid file type");
			}
			if(ImageType.valueOf(imageType) == ImageType.PROFILE_IMG) {
				Boolean profileImgUpdate = updateExistingProfileImage(customerId);
				if(!profileImgUpdate) {
					log.error("Failed to deactivate current profile image for customer id : {}", customerId);
					return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.FAILURE_CODE);
				}
			}
			// Validate file details
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
			
			 String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		        File dateFolder = new File(BASE_DIR + File.separator + currentDate);
		        if (!dateFolder.exists()) {
		            dateFolder.mkdirs();
		        }

		        // Ensure the file is saved in the correct date-based folder
		        String uniqueFileName = UUID.randomUUID() + "." + ApplicationConstants.TXT;
		        File outputFile = new File(dateFolder, uniqueFileName); // ✅ Corrected file path

		        // Encode file content in Base64
		        byte[] fileBytes = file.getBytes();
		        String base64EncodedContent = Base64.getEncoder().encodeToString(fileBytes);

		        // Write Base64 encoded content to the file
		        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
		            fos.write(base64EncodedContent.getBytes());
		        }

	        
			// Save metadata to the database
			FileDTO metadata = new FileDTO();
			metadata.setCustomerId(customerId);
			metadata.setFileName(uniqueFileName);
			metadata.setFilePath(outputFile.getAbsolutePath());
			metadata.setFileType(ImageType.valueOf(imageType));
			metadata.setFileMimeType(mimeType);
			metadata.setFileExtension(fileExtension);
			metadata.setIsActive(true);
			metadata.setIsDeleted(false);

			FileDTO fileResponse = fileRepository.save(metadata);
			
			FileResponseDTO fileResponseDTO = FileResponseDTO.builder().imageId(fileResponse.getId()).build();
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, fileResponseDTO);

		} catch (Exception e) {
			return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
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
