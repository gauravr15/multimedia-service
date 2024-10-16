package com.odin.multimedia.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.service.ImageService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    // Endpoint to fetch banners
    @GetMapping("/{type}")
    public ResponseEntity<Object> getBanners(@PathVariable("type") String type) {
        try {
            // Convert type string to ImageType enum
            ImageType imageType = ImageType.valueOf(type.toUpperCase());

            // Fetch the images as Base64 encoded strings
            ResponseDTO images = imageService.getImagesByType(imageType);

            // Return the images with a 200 OK status
            return new ResponseEntity<>(images, HttpStatus.OK); 
        } catch (IllegalArgumentException e) {
            // Invalid ImageType
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Error reading images from the file system
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
