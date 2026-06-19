/*
package com.bank.as.kafka;

import com.bank.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationProducer {
    private final KafkaTemplate<String,UserRegisteredEvent> kafkaTemplate;
    private static final String TOPIC = "user-registration-topic";

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC,event)
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
*/
