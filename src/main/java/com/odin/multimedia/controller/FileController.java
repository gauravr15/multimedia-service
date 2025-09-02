package com.odin.multimedia.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.FetchImageDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.factory.UploadFileFactory;
import com.odin.multimedia.service.FetchFileService;

@RestController
@RequestMapping(ApplicationConstants.API_VERSION + ApplicationConstants.FILE)
public class FileController {
	
	@Autowired
	private UploadFileFactory fileUpload;
	
	@Autowired
	private FetchFileService fetchFile;
	
	@PostMapping(ApplicationConstants.UPLOAD)
	public ResponseEntity<Object> uploadFile(@RequestHeader Map<String, String> headers, @RequestParam("imageType") String imageType,
			MultipartHttpServletRequest request, HttpServletResponse response) {
		System.out.println("Image type is : "+imageType);
		ResponseDTO responseDTO = fileUpload.getInstance(headers).uploadFile(request, response, headers, imageType);
		return new ResponseEntity<>(responseDTO, HttpStatus.OK);
	}

	@PostMapping(ApplicationConstants.FETCH)
	public ResponseEntity<Object> getFiles(HttpServletRequest request, @RequestBody FetchImageDTO fetchImageDTO) {
		
		ResponseDTO responseDTO = fetchFile.fetchFile(request, fetchImageDTO);
		return new ResponseEntity<>(responseDTO, HttpStatus.OK);
	}
}
