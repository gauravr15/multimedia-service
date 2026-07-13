package com.odin.multimedia.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.odin.multimedia.config.StatusUpdateKafkaProperties;
import com.odin.multimedia.dto.NotificationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatusUpdateEventPublisher {

    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    private final StatusUpdateKafkaProperties kafkaProperties;

    public void publish(NotificationDTO message, String customerKey) {
		long started = System.nanoTime();
        if (!kafkaProperties.isEnabled()) {
            log.debug("Status update Kafka publishing disabled. Skipping notificationId={} and key={}",
                    message.getNotificationId(), customerKey);
            return;
        }

		Object eventType = message.getMap() == null ? null : message.getMap().get("eventType");
		Object statusId = message.getMap() == null ? null : message.getMap().get("catalogStatusId");
		log.info("Status hint publish attempt. eventType={}, statusId={}", eventType, statusId);

        Message<NotificationDTO> kafkaMessage = MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, kafkaProperties.getTopic())
                .setHeader(KafkaHeaders.MESSAGE_KEY, customerKey)
                .setHeader(KafkaHeaders.GROUP_ID, kafkaProperties.getGroupId())
                .build();

        kafkaTemplate.send(kafkaMessage).addCallback(result -> log.info(
                        "Published status update event to topic={}, partition={}, offset={}, notificationId={}, key={}",
                        kafkaProperties.getTopic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getNotificationId(),
						customerKey),
				ex -> log.error("HINT_PUBLISH_FAILED eventType={}, statusId={}, elapsedMs={}",
						eventType, statusId, (System.nanoTime() - started) / 1_000_000L));
    }
}
