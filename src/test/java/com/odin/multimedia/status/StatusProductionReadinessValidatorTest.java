package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.odin.multimedia.config.StatusProductionReadinessValidator;
import com.odin.multimedia.service.status.StatusMediaStore;

class StatusProductionReadinessValidatorTest {

    @Test
    void developmentAllowsTransitionalStoreButProductionFailsClosed() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(false);

        assertDoesNotThrow(() -> new StatusProductionReadinessValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "development"), store).validate());

        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("production");
        assertThrows(IllegalStateException.class,
                () -> new StatusProductionReadinessValidator(production, store).validate());
    }

    @Test
    void productionRejectsInvalidReconciliationBatch() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(true);
        MockEnvironment production = new MockEnvironment()
                .withProperty("status.lifecycle-reconciliation.batch-size", "501");
        production.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class,
                () -> new StatusProductionReadinessValidator(production, store).validate());
    }
}
