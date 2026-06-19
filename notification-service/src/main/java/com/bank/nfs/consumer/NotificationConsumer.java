package com.bank.nfs.consumer;

import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import com.bank.nfs.model.Notification;
import com.bank.nfs.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {
    private final NotificationRepository repository;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_TOPIC,groupId ="notification-group")
    public void consume(NotificationEvent event) {

        Notification notification =
                Notification.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(event.getUserId())
                        .title(event.getTitle())
                        .message(event.getMessage())
                        .type(NotificationType.valueOf(event.getType()))
                        .priority(NotificationPriority.valueOf(event.getPriority()))
                        .readFlag(false)
                        .createdAt(LocalDateTime.now())
                        .build();

        repository.save(notification);
    }
}
