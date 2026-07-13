package com.odin.multimedia.config;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "status")
public class StatusDurationProperties {

    private long durationHours = 24;
    private long maxDurationHours = 168;

    @PostConstruct
    public void validate() {
        if (maxDurationHours <= 0) {
            throw new IllegalStateException("status.max-duration-hours must be positive");
        }
        if (durationHours <= 0 || durationHours > maxDurationHours) {
            throw new IllegalStateException(
                    "status.duration-hours must be between 1 and " + maxDurationHours);
        }
    }

    public Duration getDuration() {
        validate();
        return Duration.ofHours(durationHours);
    }
}
