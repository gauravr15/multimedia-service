package com.odin.multimedia.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import com.odin.multimedia.dto.ResponseDTO;

public interface VideoService {

	ResponseDTO uploadVideo(MultipartFile file, String title, String description, HttpServletRequest request);

}
