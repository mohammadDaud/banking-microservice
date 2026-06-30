package com.bank.kafka;

import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.DashboardMessage;
import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.DashboardPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditLogRepository repository;
    private final DashboardPushService dashboardPushService;

    @KafkaListener(
            topics = KafkaTopics.AUDIT_LOG_TOPIC,
            groupId = "audit-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(AuditEvent event) {

        log.info("AUDIT EVENT=========== :  {}", event.toString());

        try {
            AuditLog log = AuditLog.builder()
                    .id(UUID.randomUUID().toString())
                    // Event Metadata
                    .eventId(event.getEventId())
                    .correlationId(event.getCorrelationId())
                    .requestId(event.getRequestId())
                    .serviceName(event.getServiceName())
                    .source(event.getSource())
                    .status(event.getStatus())
                    // User
                    .userId(event.getUserId())
                    .username(event.getUsername())
                    .role(event.getRole())
                    // Audit
                    .module(AuditModule.valueOf(event.getModule()))
                    .action(AuditAction.valueOf(event.getAction()))
                    .entityId(event.getEntityId())
                    .entityType(event.getEntityType())
                    // Request
                    .requestMethod(event.getRequestMethod())
                    .requestUri(event.getRequestUri())
                    .ipAddress(event.getIpAddress())
                    // Description
                    .description(event.getDescription())
                    // Timestamp
                    .createdAt(event.getCreatedAt())
                    .build();

            repository.save(log);
            dashboardPushService.push(
                    DashboardMessage.builder()
                            .type(AuditModule.valueOf(event.getModule()).name())
                            .action(AuditAction.valueOf(event.getAction()).name())
                            .timestamp(event.getCreatedAt())
                            .data(event)
                            .build()
            );

        } catch (Exception ex) {

            log.error("Failed to consume audit event", ex);

        }
    }
}