package com.odin.multimedia.config;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.odin.multimedia.service.status.StatusMediaStore;

import lombok.RequiredArgsConstructor;

/** Prevents an explicitly production-profiled service from using unsafe status storage. */
@Component
@RequiredArgsConstructor
public class StatusProductionReadinessValidator {

    private final Environment environment;
    private final StatusMediaStore mediaStore;

    @PostConstruct
    public void validate() {
        if (!isProductionProfile()) return;
        if (!mediaStore.isDurabilityQualified()) {
            throw new IllegalStateException(
                    "Production status media storage is not durability-qualified");
        }
        int batchSize = environment.getProperty(
                "status.lifecycle-reconciliation.batch-size", Integer.class, 100);
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException(
                    "status.lifecycle-reconciliation.batch-size must be between 1 and 500");
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "production".equalsIgnoreCase(profile)
                        || "prod".equalsIgnoreCase(profile));
    }
}
