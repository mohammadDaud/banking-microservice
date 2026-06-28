package com.bank.nfs.consumer;

import com.bank.nfs.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.bank.common.events.EmailNotificationEvent;
import com.bank.common.topics.KafkaTopics;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(topics =KafkaTopics.EMAIL_NOTIFICATION_TOPIC,groupId ="notification-group")
    public void consume(EmailNotificationEvent event) {
        log.info("Received Email Event : {}",event);
        try {
            emailService.sendEmail(
                    EmailNotificationEvent.builder()
                            .to(event.getTo())
                            .subject(event.getSubject())
                            .body(event.getBody())
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed processing email event",ex);
        }
    }
}