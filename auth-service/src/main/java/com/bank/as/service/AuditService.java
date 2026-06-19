package com.bank.as.service;

import com.bank.as.model.entites.AuditLog;
import com.bank.as.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository
            auditLogRepository;

    public void audit(
            String userId,
            String username,
            String eventType,
            String ipAddress,
            Boolean success,
            String details) {

        AuditLog auditLog =
                AuditLog.builder()
                        .userId(userId)
                        .username(username)
                        .eventType(eventType)
                        .eventTime(
                                LocalDateTime.now())
                        .ipAddress(ipAddress)
                        .success(success)
                        .details(details)
                        .build();

        auditLogRepository.save(auditLog);
    }
}