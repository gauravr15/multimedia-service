package com.odin.multimedia.factory;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.service.CustomerFileUploadService;
import com.odin.multimedia.service.FileUploadService;

@Component
public class UploadFileFactory {
	
	@Autowired
	private CustomerFileUploadService customerFileUpload;
	
	public FileUploadService getInstance(Map<String, String> headers) {
		System.out.println(headers.get(ApplicationConstants.USER_TYPE));
		String customerType = headers.get(ApplicationConstants.USER_TYPE.toLowerCase()).toString();
		switch (customerType) {
		case "CUSTOMER":
			return customerFileUpload;
		default:
			return null;
		}

	}

}
