package com.odin.multimedia.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.ImageDTO;
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
        
        // Create a custom thread pool to process image reading in parallel
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        // Use CompletableFuture to process images in parallel
        List<CompletableFuture<String>> futures = images.stream()
            .map(imageData -> CompletableFuture.supplyAsync(() -> {
                try {
                    // Read the image bytes
                    byte[] imageBytes = Files.readAllBytes(Paths.get(imageData.getImagePath()));
                    // Return the Base64 encoded string
                    return Base64.getEncoder().encodeToString(imageBytes);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading image file: " + imageData.getImagePath(), e);
                }
            }, executorService)) // Use the custom thread pool
            .collect(Collectors.toList());

        // Wait for all futures to complete and collect results
        List<String> responseData = futures.stream()
            .map(CompletableFuture::join) // Join to get the result of each CompletableFuture
            .collect(Collectors.toList());

        // Shutdown the executor service
        executorService.shutdown();

        // Build and return the response
        return responseBuilder.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, responseData); 
    }

	public ImageDTO uploadImage(MultipartFile file) {
		// TODO Auto-generated method stub
		return null;
	}
}
