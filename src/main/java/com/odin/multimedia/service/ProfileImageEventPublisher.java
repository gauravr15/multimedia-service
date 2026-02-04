package com.odin.multimedia.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.odin.multimedia.config.ProfileImageKafkaProperties;
import com.odin.multimedia.dto.NotificationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageEventPublisher {

    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    private final ProfileImageKafkaProperties kafkaProperties;

        public void publish(NotificationDTO message, String customerKey) {
        if (!kafkaProperties.isEnabled()) {
            log.debug("Profile image Kafka publishing disabled. Skipping notificationId={} and key={}",
                    message.getNotificationId(), customerKey);
            return;
        }

        log.info("Publishing profile image notification to Kafka. topic={}, key={}, customerId={}", 
                kafkaProperties.getTopic(), customerKey, message.getCustomerId());

        Message<NotificationDTO> kafkaMessage = MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, kafkaProperties.getTopic())
                .setHeader(KafkaHeaders.MESSAGE_KEY, customerKey)
                .setHeader(KafkaHeaders.GROUP_ID, kafkaProperties.getGroupId())
                .build();

        kafkaTemplate.send(kafkaMessage).addCallback(result -> log.info(
                        "Published profile image event to topic={}, partition={}, offset={}, notificationId={}, key={}",
                        kafkaProperties.getTopic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getNotificationId(),
                        customerKey),
                ex -> log.error("Failed to publish profile image event for key={}, notificationId={}",
                        customerKey, message.getNotificationId(), ex));
    }
}
