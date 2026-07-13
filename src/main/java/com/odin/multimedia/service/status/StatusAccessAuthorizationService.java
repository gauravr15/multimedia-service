package com.odin.multimedia.service.status;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.enums.StatusLifecycleState;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusAccessAuthorizationService {
    private final ProfileStatusEligibilityClient profileClient;
    private final Clock clock;
	public void requireViewerReady(String viewerId) { profileClient.requireReady(viewerId); }

    public Map<String, String> authorizeUploaders(String viewerId, Collection<StatusCatalogRecord> records) {
        Set<String> uploaders = new LinkedHashSet<>();
        for (StatusCatalogRecord record : records) if (isCatalogAvailable(record, viewerId)) uploaders.add(record.getUploaderId());
        Map<String, String> decisions = profileClient.evaluate(viewerId, uploaders);
        if (decisions.containsValue("INDETERMINATE")) {
            throw new StatusAccessException(StatusAccessException.Category.DEPENDENCY_FAILURE,
                    "Eligibility could not be determined");
        }
        return decisions;
    }

    public boolean isCatalogAvailable(StatusCatalogRecord record, String viewerId) {
        return record != null && record.getLifecycleState() == StatusLifecycleState.ACTIVE
                && record.getExpiresAt() != null && clock.instant().isBefore(record.getExpiresAt())
                && record.getMediaReference() != null && !record.getMediaReference().trim().isEmpty()
                && record.getUploaderId() != null && !record.getUploaderId().equals(viewerId);
    }

    public void requireMediaAccess(String viewerId, StatusCatalogRecord record) {
		requireViewerReady(viewerId);
        if (!isCatalogAvailable(record, viewerId)) throw notFound();
        Map<String, String> decisions = authorizeUploaders(viewerId, java.util.Collections.singletonList(record));
        if (!"ALLOW".equals(decisions.get(record.getUploaderId()))) throw notFound();
    }

    private StatusAccessException notFound() {
        return new StatusAccessException(StatusAccessException.Category.NOT_FOUND, "Status unavailable");
    }
}
