package com.bank.kafka;

import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditLogRepository repository;

    @KafkaListener(
            topics = KafkaTopics.AUDIT_LOG_TOPIC,
            groupId = "audit-group"
    )
    public void consume(AuditEvent event) {
        AuditLog log = AuditLog.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(event.getUserId())
                        .username(event.getUsername())
                        .role(event.getRole())
                        .module(AuditModule.valueOf(event.getModule()))
                        .action(AuditAction.valueOf(event.getAction()))
                        .entityId(event.getEntityId())
                        .entityType(event.getEntityType())
                        .description(event.getDescription())
                        .ipAddress(event.getIpAddress())
                        .createdAt(event.getCreatedAt())
                        .build();
        repository.save(log);
    }
}
