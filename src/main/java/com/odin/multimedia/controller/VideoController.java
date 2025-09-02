package com.odin.multimedia.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.service.VideoService;

@RestController
@RequestMapping(ApplicationConstants.API_VERSION + ApplicationConstants.VIDEO)
public class VideoController {

    private final Path videoPath = Paths.get("C:/Users/gaura/Videos/Captures/pubg.mp4");
    
    @Autowired
    private VideoService videoService;

    @GetMapping
    public ResponseEntity<Resource> getVideo(@RequestParam(value = "range", required = false) String range,
            HttpServletRequest request) throws IOException {
        long videoLength = Files.size(videoPath);
        Resource videoResource = new UrlResource(videoPath.toUri());

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : videoLength - 1;

            if (start > end || start >= videoLength) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            end = Math.min(end, videoLength - 1);
            byte[] chunk = new byte[(int) (end - start + 1)];
            try (InputStream inputStream = Files.newInputStream(videoPath)) {
                inputStream.skip(start);
                inputStream.read(chunk);
            }

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + videoLength)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(chunk.length))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new ByteArrayResource(chunk));
        }

        // If no range header, return the full video (fallback)
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(videoLength))
                .body(videoResource);
    }
    
    @PostMapping(ApplicationConstants.UPLOAD)
    ResponseEntity<Object> uploadVideo(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title, 
            @RequestParam(value = "description", required = false) String description, 
            HttpServletRequest request){
    	ResponseDTO response = videoService.uploadVideo(file, title, description, request);
    	return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
