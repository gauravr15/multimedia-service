package com.odin.multimedia.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.Profile;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.utility.SearchCriteria;
import com.odin.multimedia.utility.Utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProfileRepository {

	@Value("${core.update.url}")
	private String coreUpdateUrl;

	@Autowired
	private Utility utility;

	public Profile findByMobileOrEmail(String mobile, String email) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
		searchCriteriaList.add(new SearchCriteria("mobile", ":", mobile, "OR"));
		searchCriteriaList.add(new SearchCriteria("email", ":", email, "OR"));

		// Make the REST call using your utility method
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.DETAILS, searchCriteriaList,
				HttpMethod.POST, ResponseDTO.class);

		return utility.getAnInstance(response.getData(), Profile.class);
	}

	public Profile findByMobileOrEmailAndCustomerType(String mobile, String email, String customerType) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
		searchCriteriaList.add(new SearchCriteria("mobile", ":", mobile, "OR"));
		searchCriteriaList.add(new SearchCriteria("email", ":", email, "OR"));
		searchCriteriaList.add(new SearchCriteria("customerType", ":", customerType, "AND"));

		// Make the REST call using your utility method
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.DETAILS, searchCriteriaList,
				HttpMethod.POST, ResponseDTO.class);

		return utility.getAnInstance(response.getData(), Profile.class);
	}

	public Profile findByCustomerId(String id) {
		List<SearchCriteria> searchCriteriaList = new ArrayList<>();
		searchCriteriaList.add(new SearchCriteria("customerId", ":", id, ""));

		// Make the REST call using your utility method
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.DETAILS, searchCriteriaList,
				HttpMethod.POST, ResponseDTO.class);

		return utility.getAnInstance(response.getData(), Profile.class);
	}

	public Profile update(Profile profile) {
		ResponseDTO response = utility.makeRestCall(
				coreUpdateUrl + ApplicationConstants.CUSTOMER + ApplicationConstants.UPDATE, profile, HttpMethod.POST,
				ResponseDTO.class);
		return utility.getAnInstance(response.getData(), Profile.class);
	}

}
