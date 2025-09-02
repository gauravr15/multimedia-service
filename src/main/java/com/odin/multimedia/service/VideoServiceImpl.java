package com.odin.multimedia.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.utility.ResponseObject;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${video.upload.path}")
    private String videoPath; // Inject the upload path from application.properties

    @Autowired
    private ResponseObject responseBuilder;

    @Override
    public ResponseDTO uploadVideo(MultipartFile file, String title, String description, HttpServletRequest request) {
        // Check if the file is empty
        if (file.isEmpty()) {
            return responseBuilder.buildResponse(
                    ResponseCodes.FAILURE_CODE, "File is empty");
        }

        try {
            // Create a unique file name based on the original file name
            String originalFileName = file.getOriginalFilename();
            String fileName = System.currentTimeMillis() + "_" + originalFileName;

            // Create the directory if it doesn't exist
            Path directoryPath = Paths.get(videoPath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Save the file to the specified location
            Path filePath = Paths.get(videoPath, fileName);
            file.transferTo(filePath.toFile());

            // Generate the file path URL (if required, based on the request)
            String videoUrl = request.getScheme() + "://" + request.getServerName() 
                    + ":" + request.getServerPort() + "/videos/" + fileName;

            // Return a successful response with the video URL
            return responseBuilder.buildResponse(
                    ResponseCodes.SUCCESS, ResponseCodes.SUCCESS_CODE, videoUrl);

        } catch (IOException e) {
            // Handle any file-related exceptions
            return responseBuilder.buildResponse(
                    ResponseCodes.FAILURE_CODE, "Error uploading video: ");
        }
    }
}
