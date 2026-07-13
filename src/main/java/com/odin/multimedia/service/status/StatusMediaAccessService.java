package com.odin.multimedia.service.status;

import org.springframework.stereotype.Service;
import com.odin.multimedia.entity.status.StatusCatalogRecord;
import com.odin.multimedia.repository.StatusCatalogRepository;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class StatusMediaAccessService {
    private final StatusCatalogRepository repository;
    private final StatusAccessAuthorizationService authorization;
    private final StatusMediaStore mediaStore;

    public StatusMediaAccess open(String viewerId, String statusId) {
        if (statusId == null || !statusId.matches("[0-9a-fA-F-]{36}")) throw notFound();
        StatusCatalogRecord record = repository.findById(statusId).orElseThrow(this::notFound);
        authorization.requireMediaAccess(viewerId, record);
        if (!mediaStore.exists(record.getMediaReference())) throw notFound();
        return new StatusMediaAccess(mediaStore.open(record.getMediaReference()), record.getMediaType());
    }
    private StatusAccessException notFound() {
        return new StatusAccessException(StatusAccessException.Category.NOT_FOUND, "Status unavailable");
    }
}
