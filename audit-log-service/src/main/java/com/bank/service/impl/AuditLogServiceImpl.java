package com.bank.service.impl;

import com.bank.dtos.AuditDashboardResponse;
import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;
import com.bank.dtos.AuditLogSearchRequest;
import com.bank.enums.AuditAction;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.AuditLogService;
import com.bank.specification.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Override
    public Page<AuditLogResponse> search(AuditLogSearchRequest request) {

        Sort sort = request.getSortDirection().equalsIgnoreCase("ASC")
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );

        Page<AuditLog> logs =
                repository.findAll(
                        AuditLogSpecification.search(request),
                        pageable
                );

        return logs.map(log ->
                AuditLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .username(log.getUsername())
                        .role(log.getRole())
                        .module(log.getModule().name())
                        .action(log.getAction().name())
                        .description(log.getDescription())
                        .createdAt(log.getCreatedAt())
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuditDashboardResponse getDashboardStats() {

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return AuditDashboardResponse.builder()
                .totalAuditLogs(repository.count())
                .todayAuditLogs(repository.countByCreatedAtBetween(start,end))
                .loginSuccess(repository.countByModuleAndAction("AUTH","LOGIN_SUCCESS"))
                .loginFailed(repository.countByModuleAndAction("AUTH","LOGIN_FAILED"))
                .userRegistrations(repository.countByModuleAndAction("AUTH","REGISTERED_SUCCESS"))
                .accountsCreated(repository.countByModuleAndAction("ACCOUNT","ACCOUNT_CREATED"))
                .kycApproved(repository.countByModuleAndAction("KYC","KYC_APPROVED"))
                .beneficiaryApproved(repository.countByModuleAndAction("BENEFICIARY","BENEFICIARY_APPROVED"))
                .successfulTransactions(repository.countByModuleAndAction("TRANSACTION","TRANSFER_SUCCESS"))
                .failedTransactions(repository.countByModuleAndAction("TRANSACTION","TRANSFER_FAILED"))
                .build();
    }
}