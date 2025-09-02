package com.odin.multimedia.service;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.odin.multimedia.dto.ResponseDTO;

public interface FileUploadService {

	ResponseDTO uploadFile(MultipartHttpServletRequest request, HttpServletResponse response, Map<String, String> headers, String imageType);

}
