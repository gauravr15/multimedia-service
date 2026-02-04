package com.odin.multimedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "status.image")
public class StatusImageProperties {

    /**
     * TTL for status images stored in Redis, in hours.
     */
    private long ttlHours = 24;
}
