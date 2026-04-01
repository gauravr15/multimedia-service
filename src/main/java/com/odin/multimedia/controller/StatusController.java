package com.odin.multimedia.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.odin.multimedia.constants.ApplicationConstants;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.service.StatusDeletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(ApplicationConstants.API_VERSION + "/status")
@RequiredArgsConstructor
public class StatusController {

    private final StatusDeletionService statusDeletionService;

    @DeleteMapping("/delete")
    public ResponseEntity<ResponseDTO> deleteStatus(
            @RequestHeader("customerId") String customerId,
            @RequestParam("statusKey") String statusKey) {

        log.info("[STATUS-DELETE] Received delete request. customerId={}, statusKey={}", customerId, statusKey);

        if (customerId == null || customerId.isEmpty()) {
            log.warn("[STATUS-DELETE] Missing customerId header");
            return ResponseEntity.badRequest().body(
                    ResponseDTO.builder()
                            .statusCode(1000)
                            .status("FAILURE")
                            .message("Missing customerId header")
                            .build());
        }

        if (statusKey == null || statusKey.isEmpty()) {
            log.warn("[STATUS-DELETE] Missing statusKey parameter");
            return ResponseEntity.badRequest().body(
                    ResponseDTO.builder()
                            .statusCode(1000)
                            .status("FAILURE")
                            .message("Missing statusKey parameter")
                            .build());
        }

        // Verify the status key belongs to the requesting customer
        if (!statusKey.startsWith(customerId + ":STATUS_IMG:")) {
            log.warn("[STATUS-DELETE] Ownership check failed. customerId={} does not own statusKey={}", customerId, statusKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ResponseDTO.builder()
                            .statusCode(1000)
                            .status("FAILURE")
                            .message("You can only delete your own status")
                            .build());
        }

        ResponseDTO response = statusDeletionService.deleteStatus(customerId, statusKey);
        return ResponseEntity.ok(response);
    }
}
