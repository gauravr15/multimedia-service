package com.odin.multimedia.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.AccessRights;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.utility.Utility;


@Component
public class AccessRightsRepository {

	@Value("${core.update.url}")
	private String coreUpdateUrl;

	@Autowired
	private Utility utility;

	public List<AccessRights> findAllAccessRights() {
	    
	    ResponseDTO response = utility.makeRestCall(
	    		coreUpdateUrl + ApplicationConstants.FETCH + ApplicationConstants.ALL + ApplicationConstants.ACCESS_RIGHTS, 
	            null, 
	            HttpMethod.POST, 
	            ResponseDTO.class
	    );
	    
	    List<AccessRights> address =  utility.getInstances(response, AccessRights.class);
	    return address;
	}
}
