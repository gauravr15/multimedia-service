package com.odin.multimedia.service;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.odin.multimedia.config.StatusImageProperties;
import com.odin.multimedia.dto.StatusMediaDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusImageService {

    private static final String STATUS_IMG_KEY_PART = ":STATUS_IMG:";
    private static final String AVAILABLE_STATUS_SUFFIX = ":AVAILABLE_STATUS";
    private static final String FILE_PATH_MAPPING_KEY = "status:filepaths";

    private final StringRedisTemplate redisTemplate;
    private final StatusImageProperties properties;

    /**
     * Stores status image metadata in Redis and updates the index.
     * 
     * @param customerId The ID of the customer
     * @param filePath   The physical path of the stored file
     * @return The generated status key
     */
    public String storeStatusImage(String customerId, String filePath) {
        long timestamp = System.currentTimeMillis();
        String statusKey = customerId + STATUS_IMG_KEY_PART + timestamp;
        String indexKey = customerId + AVAILABLE_STATUS_SUFFIX;

        try {
            // 1. Store the file path with TTL
            redisTemplate.opsForValue().set(statusKey, filePath, Duration.ofHours(properties.getTtlHours()));

            // 2. Store in mapping for cleanup (since we can't get value of expired key)
            redisTemplate.opsForHash().put(FILE_PATH_MAPPING_KEY, statusKey, filePath);

            // 3. Append to the available status list
            redisTemplate.opsForList().rightPush(indexKey, statusKey);

            log.info("Stored status image. customerId={}, statusKey={}, ttlHours={}", 
                    customerId, statusKey, properties.getTtlHours());
            
            return statusKey;
        } catch (DataAccessException dae) {
            log.error("Redis error while storing status image for customerId={}", customerId, dae);
            throw dae;
        } catch (Exception ex) {
            log.error("Unexpected error while storing status image for customerId={}", customerId, ex);
            throw new RuntimeException("Failed to store status image", ex);
        }
    }

    /**
     * Fetches all active status media for a customer.
     * 
     * @param customerId The ID of the customer
     * @return List of status media DTOs
     */
    public List<StatusMediaDTO> fetchStatusMedia(String customerId) {
        List<StatusMediaDTO> results = new ArrayList<>();
        String indexKey = customerId + AVAILABLE_STATUS_SUFFIX;

        try {
            List<String> statusKeys = redisTemplate.opsForList().range(indexKey, 0, -1);
            if (statusKeys == null || statusKeys.isEmpty()) {
                log.debug("No active statuses found for customerId={}", customerId);
                return results;
            }

            for (String key : statusKeys) {
                try {
                    String filePath = redisTemplate.opsForValue().get(key);
                    if (filePath == null) {
                        // Key might have expired but still in list (cleanup listener will handle this eventually, 
                        // but we skip it here for safety)
                        continue;
                    }
                    
                    File file = new File(filePath);
                    if (!file.exists()) {
                        log.warn("File not found at path: {}", filePath);
                        continue;
                    }

                    String fileData = new String(Files.readAllBytes(file.toPath()), "UTF-8");
                    String statusId = key.substring(key.lastIndexOf(":") + 1);
                    
                    results.add(StatusMediaDTO.builder()
                            .statusId(statusId)
                            .fileData(fileData)
                            .build());
                } catch (Exception ex) {
                    log.error("Failed to process status key: {}", key, ex);
                }
            }
        } catch (Exception e) {
            log.error("Error fetching status media for customerId={}", customerId, e);
        }

        return results;
    }

    /**
     * Retrieves the file path for a specific status image key.
     * 
     * @param statusKey The full status key (e.g., customerId:STATUS_IMG:timestamp)
     * @return The file path if found and active, null otherwise
     */
    public String getFilePathByKey(String statusKey) {
        try {
            return redisTemplate.opsForValue().get(statusKey);
        } catch (Exception e) {
            log.error("Error fetching file path for statusKey={}", statusKey, e);
            return null;
        }
    }

    /**
     * Deletes a status image: removes the physical file, Redis TTL key,
     * file-path mapping hash entry, and the AVAILABLE_STATUS list entry.
     *
     * @param customerId The owner of the status
     * @param statusKey  The full status key (e.g., customerId:STATUS_IMG:timestamp)
     * @return true if the status existed and was cleaned up, false if not found
     */
    public boolean deleteStatusImage(String customerId, String statusKey) {
        String indexKey = customerId + AVAILABLE_STATUS_SUFFIX;

        try {
            // 1. Get file path from mapping hash (survives TTL expiry)
            String filePath = (String) redisTemplate.opsForHash().get(FILE_PATH_MAPPING_KEY, statusKey);

            if (filePath == null) {
                // Fall back to the TTL key itself
                filePath = redisTemplate.opsForValue().get(statusKey);
            }

            if (filePath == null) {
                log.warn("[STATUS-DELETE] No file path found in Redis for statusKey={}", statusKey);
                return false;
            }

            // 2. Delete the physical file from disk
            File file = new File(filePath);
            if (file.exists()) {
                boolean fileDeleted = file.delete();
                if (fileDeleted) {
                    log.info("[STATUS-DELETE] Deleted file from disk. path={}", filePath);
                } else {
                    log.warn("[STATUS-DELETE] Failed to delete file from disk. path={}", filePath);
                }
            } else {
                log.debug("[STATUS-DELETE] File already absent on disk. path={}", filePath);
            }

            // 3. Delete the TTL key
            Boolean keyDeleted = redisTemplate.delete(statusKey);
            log.info("[STATUS-DELETE] Deleted TTL key. statusKey={}, result={}", statusKey, keyDeleted);

            // 4. Remove from file-path mapping hash
            Long hashRemoved = redisTemplate.opsForHash().delete(FILE_PATH_MAPPING_KEY, statusKey);
            log.info("[STATUS-DELETE] Removed from filepaths hash. statusKey={}, removedCount={}", statusKey, hashRemoved);

            // 5. Remove from AVAILABLE_STATUS list
            Long listRemoved = redisTemplate.opsForList().remove(indexKey, 0, statusKey);
            log.info("[STATUS-DELETE] Removed from AVAILABLE_STATUS list. indexKey={}, removedCount={}", indexKey, listRemoved);

            // 6. Clean up empty list key
            Long listSize = redisTemplate.opsForList().size(indexKey);
            if (listSize == null || listSize == 0) {
                redisTemplate.delete(indexKey);
                log.info("[STATUS-DELETE] AVAILABLE_STATUS list empty, deleted indexKey={}", indexKey);
            }

            return true;

        } catch (Exception e) {
            log.error("[STATUS-DELETE] Error deleting status. customerId={}, statusKey={}", customerId, statusKey, e);
            return false;
        }
    }
}
