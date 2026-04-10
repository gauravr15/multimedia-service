package com.odin.multimedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "profile.image.kafka")
public class ProfileImageKafkaProperties {

    /**
     * Toggle to enable or disable publishing profile image upload events to Kafka.
     */
    private boolean enabled = true;

    /**
     * Target topic name for profile image events.
     */
    private String topic = "profile-image-updates";

    /**
     * Logical consumer group identifier to route/segregate profile image events.
     * This is added as a message header to allow consumers to join the right group.
     */
    private String groupId = "profile-image-updates-group";
}
