package com.odin.multimedia.service;

import javax.servlet.http.HttpServletRequest;

import com.odin.multimedia.dto.FetchImageDTO;
import com.odin.multimedia.dto.ResponseDTO;

public interface FetchFileService {

	ResponseDTO fetchFile(HttpServletRequest request, FetchImageDTO fetchImageDTO);

}
