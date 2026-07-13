package com.odin.multimedia.status;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import com.odin.multimedia.config.StatusDurationProperties;

class StatusDurationPropertiesTest {
    @Test void validatesDefaultAndConfiguredDuration() {
        StatusDurationProperties p = new StatusDurationProperties();
        assertEquals(Duration.ofHours(24), p.getDuration());
        p.setDurationHours(48);
        assertEquals(Duration.ofHours(48), p.getDuration());
    }
    @Test void rejectsZeroNegativeAndExcessiveDuration() {
        StatusDurationProperties p = new StatusDurationProperties();
        p.setDurationHours(0); assertThrows(IllegalStateException.class, p::validate);
        p.setDurationHours(-1); assertThrows(IllegalStateException.class, p::validate);
        p.setDurationHours(169); assertThrows(IllegalStateException.class, p::validate);
    }
}
