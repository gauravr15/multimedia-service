package com.odin.multimedia.service;

import java.io.File;
import java.nio.file.Files;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusImageExpiryListener implements MessageListener {

    private static final String STATUS_IMG_KEY_PART = ":STATUS_IMG:";
    private static final String AVAILABLE_STATUS_SUFFIX = ":AVAILABLE_STATUS";
    private static final String FILE_PATH_MAPPING_KEY = "status:filepaths";

    private final StringRedisTemplate redisTemplate;
    private final StatusDeletionService statusDeletionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Redis key expired: {}", expiredKey);

        if (expiredKey.contains(STATUS_IMG_KEY_PART)) {
            handleStatusExpiry(expiredKey);
        }
    }

    private void handleStatusExpiry(String statusKey) {
        try {
            log.info("Handling expiry for status key: {}", statusKey);

            // 1. Get file path from mapping
            String filePath = (String) redisTemplate.opsForHash().get(FILE_PATH_MAPPING_KEY, statusKey);
            
            if (filePath != null) {
                // 2. Delete the actual file
                File file = new File(filePath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("Successfully deleted expired status file: {}", filePath);
                    } else {
                        log.warn("Failed to delete expired status file: {}", filePath);
                    }
                } else {
                    log.debug("Status file already gone or never existed: {}", filePath);
                }

                // 3. Remove from mapping
                redisTemplate.opsForHash().delete(FILE_PATH_MAPPING_KEY, statusKey);
            }

            // 4. Remove from AVAILABLE_STATUS list
            // Key format: customerId:STATUS_IMG:timestamp
            String customerId = statusKey.split(":")[0];
            String indexKey = customerId + AVAILABLE_STATUS_SUFFIX;
            
            Long removedCount = redisTemplate.opsForList().remove(indexKey, 0, statusKey);
            log.info("Removed status key {} from index {}. Count: {}", statusKey, indexKey, removedCount);

            // 5. If the list is now empty, remove the AVAILABLE_STATUS key itself
            Long listSize = redisTemplate.opsForList().size(indexKey);
            if (listSize == null || listSize == 0) {
                redisTemplate.delete(indexKey);
                log.info("Index key {} was empty and has been deleted", indexKey);
            }

            // 6. Notify all viewers via Kafka so their devices remove the expired status
            log.info("[STATUS-EXPIRY] Publishing STATUS_DELETE notifications for expired statusKey={}", statusKey);
            statusDeletionService.publishStatusDeleteNotifications(customerId, statusKey);

        } catch (Exception e) {
            log.error("Error during status expiry cleanup for key: {}", statusKey, e);
        }
    }
}
