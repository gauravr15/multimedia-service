package com.odin.multimedia.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.FileDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.utility.SearchCriteria;
import com.odin.multimedia.utility.Utility;

@Component
public class FileRepository {

	@Value("${core.data.url}")
	private String coreDataUrl;

	@Value("${core.update.url}")
	private String coreUpdateUrl;

	@Autowired
	private Utility utility;

	public FileDTO save(FileDTO fileData) {

		ResponseDTO response = utility.makeRestCall(
				coreDataUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.FILE + ApplicationConstants.SAVE,
				fileData, HttpMethod.POST, ResponseDTO.class);
		return utility.getAnInstance(response.getData(), FileDTO.class);
	}

	public List<FileDTO> findByfileTypeAndCustomerIdAndIsActive(String fileType, String customerId, Boolean isActive) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
		searchCriteriaList.add(new SearchCriteria("fileType", ":", fileType, "AND"));
		searchCriteriaList.add(new SearchCriteria("customerId", ":", customerId, "AND"));
		searchCriteriaList.add(new SearchCriteria("isActive", ":", isActive, "AND"));

		// Make the REST call using your utility method
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.FETCH + ApplicationConstants.FILE,
				searchCriteriaList, HttpMethod.POST, ResponseDTO.class);

		return utility.getInstances(response, FileDTO.class);
	}

	public FileDTO update(FileDTO file) {
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.UPDATE + ApplicationConstants.FILE,
				file, HttpMethod.POST, ResponseDTO.class);
		return utility.getAnInstance(response.getData(), FileDTO.class);
	}

	public FileDTO findByfileTypeAndImageIdAndIsActive(ImageType imageType, Long imageId, boolean isActive) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
		searchCriteriaList.add(new SearchCriteria("fileType", ":", imageType.name(), "AND"));
		searchCriteriaList.add(new SearchCriteria("id", ":", imageId, "AND"));
		searchCriteriaList.add(new SearchCriteria("isActive", ":", isActive, "AND"));

		// Make the REST call using your utility method
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.FETCH + ApplicationConstants.FILE,
				searchCriteriaList, HttpMethod.POST, ResponseDTO.class);

		return utility.getInstances(response, FileDTO.class).get(0);
	}
}
