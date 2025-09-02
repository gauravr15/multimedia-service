package com.odin.multimedia.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.ImageDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.enums.ImageType;
import com.odin.multimedia.service.ImageService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(ApplicationConstants.API_VERSION + ApplicationConstants.IMAGES)
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
    
	@PostMapping(ApplicationConstants.UPLOAD)
	public ResponseEntity<Object> uploadImage(@RequestParam("file") MultipartFile file) {
		ImageDTO savedImage = imageService.uploadImage(file);
		return new ResponseEntity<>(savedImage, HttpStatus.OK);

	}

//    @GetMapping("/image/{id}")
//    public ResponseEntity<Object> getImageDetails(@PathVariable Long id) {
//        return imageService.getImage(id)
//                .map(image -> ResponseEntity.ok().body(image))
//                .orElse(ResponseEntity.notFound().build());
//    }
    
}
