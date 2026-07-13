package com.odin.multimedia.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.dto.StatusFeedPage;
import com.odin.multimedia.enums.StatusMediaType;
import com.odin.multimedia.service.status.StatusAccessException;
import com.odin.multimedia.service.status.StatusFeedService;
import com.odin.multimedia.service.status.StatusMediaAccess;
import com.odin.multimedia.service.status.StatusMediaAccessService;
import com.odin.multimedia.service.status.AuthoritativeStatusDeletionService;
import com.odin.multimedia.service.status.StatusDeletionNotFoundException;
import com.odin.multimedia.utility.ResponseObject;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/statuses")
@RequiredArgsConstructor
public class StatusFeedController {
    private final StatusFeedService feedService;
    private final StatusMediaAccessService mediaService;
    private final ResponseObject responseObject;
    private final AuthoritativeStatusDeletionService deletionService;

    @GetMapping("/active")
    public ResponseEntity<ResponseDTO> active(@RequestHeader("customerId") String viewerId,
            @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        if (viewerId == null || viewerId.trim().isEmpty()) return ResponseEntity.badRequest().body(failure());
        try {
            StatusFeedPage page = feedService.getFeed(viewerId, cursor, limit);
            return ResponseEntity.ok(responseObject.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE, page));
        } catch (StatusAccessException exception) {
			HttpStatus status = exception.getCategory() == StatusAccessException.Category.INVALID_REQUEST
					? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
			return ResponseEntity.status(status).body(exception.getCategory() == StatusAccessException.Category.REPAIR_REQUIRED
					? repairRequired() : failure());
        }
    }

    @GetMapping("/{statusId}/media")
    public ResponseEntity<?> media(@RequestHeader("customerId") String viewerId,
            @PathVariable String statusId) {
        if (viewerId == null || viewerId.trim().isEmpty()) return ResponseEntity.badRequest().body(failure());
        try {
            StatusMediaAccess access = mediaService.open(viewerId, statusId);
            MediaType type = access.getMediaType() == StatusMediaType.VIDEO
                    ? MediaType.parseMediaType("video/mp4") : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(type).body(new InputStreamResource(access.getStream()));
        } catch (StatusAccessException exception) {
			if (exception.getCategory() == StatusAccessException.Category.REPAIR_REQUIRED) {
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(repairRequired());
			}
            HttpStatus status = exception.getCategory() == StatusAccessException.Category.DEPENDENCY_FAILURE
                    ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(failure());
        }
    }

    @DeleteMapping("/{statusId}")
    public ResponseEntity<ResponseDTO> delete(@RequestHeader("customerId") String ownerId,
            @PathVariable String statusId) {
        if (ownerId == null || ownerId.trim().isEmpty()) return ResponseEntity.badRequest().body(failure());
        try {
            return ResponseEntity.ok(responseObject.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE,
                    deletionService.delete(ownerId, statusId)));
        } catch (StatusDeletionNotFoundException unavailable) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(failure());
        }
    }

    private ResponseDTO failure() {
        return ResponseDTO.builder().statusCode(ResponseCodes.FAILURE_CODE).status("FAILURE")
                .message("Status unavailable").build();
    }
	private ResponseDTO repairRequired() {
		return ResponseDTO.builder().statusCode(ResponseCodes.FAILURE_CODE).status("REPAIR_REQUIRED")
				.message("STATUS_PROFILE_REPAIR_REQUIRED").build();
	}
}
