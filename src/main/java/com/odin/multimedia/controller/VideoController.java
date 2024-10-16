package com.odin.multimedia.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/video")
public class VideoController {

    private final Path videoPath = Paths.get("C:/Users/gaura/Videos/Captures/pubg.mp4");

    @GetMapping
    public ResponseEntity<Resource> getVideo(@RequestParam(value = "range", required = false) String range) throws IOException {
        Resource videoResource = new UrlResource(videoPath.toUri());
        long videoLength = Files.size(videoPath);

        if (range != null && range.startsWith("bytes=")) {
            String[] ranges = range.substring("bytes=".length()).split("-");
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
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + videoLength)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(end - start + 1))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(new ByteArrayResource(chunk));
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(videoLength))
                    .body(videoResource);
        }
    }
}
