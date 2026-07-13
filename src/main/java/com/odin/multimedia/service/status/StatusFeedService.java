package com.odin.multimedia.service.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.odin.multimedia.dto.StatusFeedItem;
import com.odin.multimedia.dto.StatusFeedPage;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.StatusLifecycleState;
import com.odin.multimedia.repository.StatusCatalogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service @RequiredArgsConstructor
public class StatusFeedService {
    public static final int DEFAULT_LIMIT = 20, MAX_LIMIT = 50, SCAN_MULTIPLIER = 5;
    private final StatusCatalogRepository repository;
    private final StatusAccessAuthorizationService authorization;
    private final Clock clock;

    public StatusFeedPage getFeed(String viewerId, String cursor, Integer requestedLimit) {
		authorization.requireViewerReady(viewerId);
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) throw invalid("Invalid limit");
        Cursor decoded = decode(cursor);
        int cap = limit * SCAN_MULTIPLIER;
        List<StatusCatalogRecord> candidates = repository.findFeedCandidates(StatusLifecycleState.ACTIVE,
                clock.instant(), decoded == null ? null : decoded.createdAt,
                decoded == null ? null : decoded.statusId, PageRequest.of(0, cap));
        Map<String, String> decisions = authorization.authorizeUploaders(viewerId, candidates);
        List<StatusFeedItem> items = new ArrayList<>();
        StatusCatalogRecord lastScanned = null;
        int denied = 0;
        for (StatusCatalogRecord record : candidates) {
            lastScanned = record;
            if (!authorization.isCatalogAvailable(record, viewerId)) { denied++; continue; }
            if (!"ALLOW".equals(decisions.get(record.getUploaderId()))) { denied++; continue; }
            items.add(new StatusFeedItem(record.getStatusId(), record.getUploaderId(), record.getMediaType(),
                    record.getCreatedAt(), record.getExpiresAt(), true,
                    "/v1/statuses/" + record.getStatusId() + "/media"));
            if (items.size() == limit) break;
        }
        boolean more = lastScanned != null && (candidates.size() == cap
                || candidates.indexOf(lastScanned) < candidates.size() - 1);
        String next = more ? encode(lastScanned) : null;
        log.info("Status feed completed. viewerId={}, requestedLimit={}, candidateCount={}, allowedCount={}, deniedCount={}, scannedCount={}, hasNext={}",
                viewerId, limit, candidates.size(), items.size(), denied,
                lastScanned == null ? 0 : candidates.indexOf(lastScanned) + 1, next != null);
        return new StatusFeedPage(items, next);
    }

    private String encode(StatusCatalogRecord record) {
        String raw = record.getCreatedAt().toString() + "|" + record.getStatusId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
    private Cursor decode(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", -1);
            if (parts.length != 2 || !parts[1].matches("[0-9a-fA-F-]{36}")) throw new IllegalArgumentException();
            return new Cursor(Instant.parse(parts[0]), parts[1]);
        } catch (Exception exception) { throw invalid("Invalid cursor"); }
    }
    private StatusAccessException invalid(String message) {
        return new StatusAccessException(StatusAccessException.Category.INVALID_REQUEST, message);
    }
    private static final class Cursor { final Instant createdAt; final String statusId; Cursor(Instant t, String id){createdAt=t;statusId=id;} }
}
