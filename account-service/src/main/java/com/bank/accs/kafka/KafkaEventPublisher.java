package com.bank.accs.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, Object event) {
        kafkaTemplate.send(topic,event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                    } else {
                        System.out.println(
                                "Message sent successfully");
                    }
                });
    }
}