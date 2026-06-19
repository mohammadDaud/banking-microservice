/*
package com.bank.as.kafka;

import com.bank.common.events.EmailNotificationEvent;
import com.bank.common.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String,EmailNotificationEvent> kafkaTemplate;

    public void sendEmailEvent( EmailNotificationEvent event) {

        kafkaTemplate.send(KafkaTopics.EMAIL_NOTIFICATION_TOPIC, event)
                .whenComplete((result, ex) -> {

                    if (ex != null) {
                        ex.printStackTrace();
                    } else {
                        System.out.println("Message sent successfully");
                    }
                });
    }
}
*/
