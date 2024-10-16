package com.odin.multimedia.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.entity.ImageData;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.repository.ImageDataRepository;
import com.odin.multimedia.utility.ResponseObject;

@Service
public class ImageService {

    @Autowired
    private ImageDataRepository imageDataRepository;
    
    @Autowired
    private ResponseObject responseBuilder;

    // Fetch images by ImageType and return them as base64-encoded strings
    public ResponseDTO getImagesByType(ImageType imageType) throws IOException {
        // Fetch images from the database, sorted by sequence
        List<ImageData> images = imageDataRepository.findByImageTypeAndIsActiveTrueAndIsDeletedFalseOrderBySequenceAsc(imageType);
        List<String> responseData = images.stream()
                .map(imageData -> {
                    try {
                        byte[] imageBytes = Files.readAllBytes(Paths.get(imageData.getImagePath()));
                        return Base64.getEncoder().encodeToString(imageBytes);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading image file: " + imageData.getImagePath(), e);
                    }
                })
                .collect(Collectors.toList());
        return responseBuilder.buildResponse(LanguageConstants.EN,ResponseCodes.SUCCESS_CODE, responseData); 
    }
}
