package com.bank.us.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object event) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic cannot be null.");
        }

        if (event == null) {
            throw new IllegalArgumentException("Kafka event cannot be null.");
        }
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed. Topic={}, EventType={}", topic, event.getClass().getSimpleName(), ex);
                        return;
                    }
                    log.info("Kafka message published successfully. Topic={}, Partition={}, Offset={}, EventType={}", topic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), event.getClass().getSimpleName()
                    );
                });
    }
}