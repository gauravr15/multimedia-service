package com.odin.multimedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "status.update.kafka")
public class StatusUpdateKafkaProperties {
    private boolean enabled = false;
    private String topic = "status.update.notification.message";
    private String groupId = "status-update-notification-group";
}
