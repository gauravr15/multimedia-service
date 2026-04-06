package com.odin.multimedia.service;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.odin.multimedia.constants.ResponseCodes;
import com.odin.multimedia.constants.LanguageConstants;
import com.odin.multimedia.dto.NotificationDTO;
import com.odin.multimedia.dto.ResponseDTO;
import com.odin.multimedia.enums.NotificationChannel;
import com.odin.multimedia.repository.ProfileRepository;
import com.odin.multimedia.utility.ResponseObject;
import com.odin.multimedia.utility.Utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for deleting a status image.
 * Cleans up the physical file, Redis entries, and notifies viewers via Kafka.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusDeletionService {

    private final StatusImageService statusImageService;
    private final StatusUpdateEventPublisher statusUpdateEventPublisher;
    private final ProfileRepository profileRepo;
    private final ResponseObject responseObj;
    private final Utility utility;

    @Value("${profile.service.url}")
    private String profileServiceUrl;

    private static final String PROFILE_STATUS_ENDPOINT = "status/notify-upload";

    /**
     * Deletes a status image: removes from Redis + disk, then notifies viewers.
     *
     * @param customerId the owner of the status
     * @param statusKey  the full Redis key (e.g. customerId:STATUS_IMG:timestamp)
     * @return standard ResponseDTO
     */
    public ResponseDTO deleteStatus(String customerId, String statusKey) {
        log.info("[STATUS-DELETE] Starting deletion. customerId={}, statusKey={}", customerId, statusKey);

        try {
            // 1. Delete from Redis and disk
            boolean deleted = statusImageService.deleteStatusImage(customerId, statusKey);
            if (!deleted) {
                log.warn("[STATUS-DELETE] Status not found or already deleted. customerId={}, statusKey={}", customerId, statusKey);
                return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.NO_DATA_FOUND);
            }

            log.info("[STATUS-DELETE] Successfully deleted status from Redis and disk. statusKey={}", statusKey);

            // 2. Notify viewers about the deletion via Kafka
            publishStatusDeleteNotifications(customerId, statusKey);

            return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.SUCCESS_CODE);

        } catch (Exception e) {
            log.error("[STATUS-DELETE] Unexpected error during deletion. customerId={}, statusKey={}", customerId, statusKey, e);
            return responseObj.buildResponse(LanguageConstants.EN, ResponseCodes.EXCEPTION_CODE);
        }
    }

    /**
     * Fetches the viewer list from profile-service and publishes STATUS_DELETE
     * notifications via Kafka so that each viewer's device removes the status locally.
     * Package-private so that {@link StatusImageExpiryListener} can call it on Redis TTL expiry.
     */
    void publishStatusDeleteNotifications(String uploaderCustomerId, String statusKey) {
        log.info("[STATUS-DELETE] Fetching viewer list for delete notification. uploaderCustomerId={}", uploaderCustomerId);

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("customerId", uploaderCustomerId);

            ResponseDTO response = utility.makeRestCall(
                    profileServiceUrl.concat(PROFILE_STATUS_ENDPOINT),
                    requestBody,
                    HttpMethod.POST,
                    ResponseDTO.class);

            if (response != null && ResponseCodes.SUCCESS_CODE.equals(response.getStatusCode()) && response.getData() != null) {
                List<Long> allowedCustomerIds = utility.getInstances(response, Long.class);
                log.info("[STATUS-DELETE] Found {} viewers to notify. uploaderCustomerId={}", allowedCustomerIds.size(), uploaderCustomerId);

                String senderMobile = null;
                try {
                    senderMobile = profileRepo.findByCustomerId(uploaderCustomerId).getMobile();
                } catch (Exception e) {
                    log.warn("[STATUS-DELETE] Could not fetch uploader mobile. uploaderCustomerId={}", uploaderCustomerId, e);
                }

                for (Long viewerId : allowedCustomerIds) {
                    Map<String, Object> notificationMap = new HashMap<>();
                    notificationMap.put("files", Collections.singletonList(statusKey));
                    notificationMap.put("senderCustomerId", uploaderCustomerId);
                    notificationMap.put("statusDeleteSignal", "STATUS_DELETE");
                    if (senderMobile != null) {
                        notificationMap.put("senderMobile", senderMobile);
                        notificationMap.put("senderPhone", senderMobile);
                    }

                    NotificationDTO notification = NotificationDTO.builder()
                            .customerId(viewerId)
                            .notificationId(1L)
                            .channel(NotificationChannel.INAPP)
                            .map(notificationMap)
                            .build();

                    statusUpdateEventPublisher.publish(notification, String.valueOf(viewerId));
                }

                log.info("[STATUS-DELETE] Published delete notifications to {} viewers. statusKey={}", allowedCustomerIds.size(), statusKey);
            } else {
                log.warn("[STATUS-DELETE] Profile service returned non-success or empty data. uploaderCustomerId={}", uploaderCustomerId);
            }
        } catch (Exception ex) {
            log.error("[STATUS-DELETE] Failed to publish delete notifications. uploaderCustomerId={}", uploaderCustomerId, ex);
        }
    }
}
