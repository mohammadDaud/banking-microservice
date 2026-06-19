package com.bank.service.impl;

import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository repository;

    @Override
    public void save(AuditLogRequest request) {
        AuditLog log =
                AuditLog.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(request.getUserId())
                        .username(request.getUsername())
                        .role(request.getRole())
                        .module(request.getModule())
                        .action(request.getAction())
                        .entityId(request.getEntityId())
                        .entityType(request.getEntityType())
                        .description(request.getDescription())
                        .ipAddress(request.getIpAddress())
                        .createdAt(LocalDateTime.now())
                        .build();

        repository.save(log);
    }

    @Override
    public List<AuditLogResponse> findAll() {

        return repository.findAll()
                .stream()
                .map(log ->
                        AuditLogResponse.builder()
                                .id(log.getId())
                                .userId(log.getUserId())
                                .username(log.getUsername())
                                .role(log.getRole())
                                .module(log.getModule().name())
                                .action(log.getAction().name())
                                .description(log.getDescription())
                                .createdAt(log.getCreatedAt())
                                .build())
                .toList();
    }
}