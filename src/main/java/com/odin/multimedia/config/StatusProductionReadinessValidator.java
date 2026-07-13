package com.odin.multimedia.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.odin.multimedia.service.status.StatusMediaStore;

import lombok.extern.slf4j.Slf4j;

/** Enforces durable status storage when explicitly required by deployment configuration. */
@Slf4j
@Component
public class StatusProductionReadinessValidator {

    private final StatusMediaStore mediaStore;
    private final boolean requireDurableStorage;
    private final int reconciliationBatchSize;

    public StatusProductionReadinessValidator(StatusMediaStore mediaStore,
            @Value("${status.media.require-durable-storage:false}") boolean requireDurableStorage,
            @Value("${status.lifecycle-reconciliation.batch-size:100}") int reconciliationBatchSize) {
        this.mediaStore = mediaStore;
        this.requireDurableStorage = requireDurableStorage;
        this.reconciliationBatchSize = reconciliationBatchSize;
    }

    @PostConstruct
    public void validate() {
        if (reconciliationBatchSize < 1 || reconciliationBatchSize > 500) {
            throw new IllegalStateException(
                    "status.lifecycle-reconciliation.batch-size must be between 1 and 500");
        }
        if (!requireDurableStorage) {
            if (!mediaStore.isDurabilityQualified()) {
                log.warn("Status media durability enforcement is disabled. "
                        + "The configured media store is not production-durable; "
                        + "status media may be lost after restart or unavailable across replicas.");
            }
            return;
        }
        if (!mediaStore.isDurabilityQualified()) {
            throw new IllegalStateException(
                    "Durable status media storage is required but the configured store is not qualified");
        }
    }
}
