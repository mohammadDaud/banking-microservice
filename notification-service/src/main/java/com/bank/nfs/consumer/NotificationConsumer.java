package com.bank.nfs.consumer;

import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.nfs.dtos.DashboardMessage;
import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import com.bank.nfs.model.Notification;
import com.bank.nfs.repository.NotificationRepository;
import com.bank.nfs.service.DashboardPushService;
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
    private final DashboardPushService  dashboardPushService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_TOPIC,groupId ="notification-group")
    public void consume(NotificationEvent event) {

        log.info("Received Notification Event : eventId={}, correlationId={}",
                event.getEventId(),
                event.getCorrelationId()
        );

        Notification notification =
                Notification.builder()
                        .id(UUID.randomUUID().toString())
                        .eventId(event.getEventId())
                        .correlationId(event.getCorrelationId())
                        .requestId(event.getRequestId())
                        .serviceName(event.getServiceName())
                        .status(event.getStatus())
                        .userId(event.getUserId())
                        .title(event.getTitle())
                        .message(event.getMessage())
                        .type(NotificationType.valueOf(event.getType()))
                        .priority(NotificationPriority.valueOf(event.getPriority()))
                        .readFlag(false)
                        .createdAt(
                                event.getCreatedAt() != null
                                        ? event.getCreatedAt()
                                        : LocalDateTime.now())

                        .build();

        dashboardPushService.push(
                DashboardMessage.builder()
                        .type(event.getSource().name())
                        .action(event.getStatus().name())
                        .timestamp(event.getCreatedAt())
                        .data(event)
                        .build()
        );
        if (repository.existsByEventId(event.getEventId())) {
            log.info("Notification already processed : {}",event.getEventId());
            return;
        }
        repository.save(notification);
    }
}
