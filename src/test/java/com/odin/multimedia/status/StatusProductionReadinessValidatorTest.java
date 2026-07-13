package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import com.odin.multimedia.config.StatusProductionReadinessValidator;
import com.odin.multimedia.service.status.StatusMediaStore;

class StatusProductionReadinessValidatorTest {

    @Test
    void requirementEnabledAllowsQualifiedStore() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(true);

        assertDoesNotThrow(() -> new StatusProductionReadinessValidator(
                store, true, 100).validate());
    }

    @Test
    void requirementEnabledRejectsUnqualifiedStore() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> new StatusProductionReadinessValidator(store, true, 100).validate());
    }

    @Test
    void requirementDisabledAllowsUnqualifiedStore() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(false);

        assertDoesNotThrow(() -> new StatusProductionReadinessValidator(
                store, false, 100).validate());
    }

    @Test
    void requirementEnabledRejectsInvalidReconciliationBatch() {
        StatusMediaStore store = mock(StatusMediaStore.class);
        when(store.isDurabilityQualified()).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> new StatusProductionReadinessValidator(store, true, 501).validate());
    }
}
