package com.odin.multimedia.service.status;

import com.odin.multimedia.entity.status.StatusCatalogRecord;

public final class StatusUploadResult {
    private final StatusCatalogRecord record;
    private final boolean newlyActivated;

    public StatusUploadResult(StatusCatalogRecord record, boolean newlyActivated) {
        this.record = record;
        this.newlyActivated = newlyActivated;
    }

    public StatusCatalogRecord getRecord() { return record; }
    public boolean isNewlyActivated() { return newlyActivated; }
}
