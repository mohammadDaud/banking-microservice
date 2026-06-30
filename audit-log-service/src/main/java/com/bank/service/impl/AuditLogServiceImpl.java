package com.bank.service.impl;

import com.bank.dtos.*;
import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
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
                .loginSuccess(repository.countByModuleAndAction(AuditModule.AUTH,AuditAction.LOGIN_SUCCESS))
                .loginFailed(repository.countByModuleAndAction(AuditModule.AUTH,AuditAction.LOGIN_FAILED))
                .userRegistrations(repository.countByModuleAndAction(AuditModule.AUTH,AuditAction.REGISTERED_SUCCESS))
                .accountsCreated(repository.countByModuleAndAction(AuditModule.ACCOUNT,AuditAction.ACCOUNT_CREATED))
                .kycApproved(repository.countByModuleAndAction(AuditModule.KYC,AuditAction.KYC_APPROVED))
                .beneficiaryApproved(repository.countByModuleAndAction(AuditModule.BENEFICIARY,AuditAction.BENEFICIARY_APPROVED))
                .successfulTransactions(repository.countByModuleAndAction(AuditModule.TRANSACTION,AuditAction.TRANSFER_SUCCESS))
                .failedTransactions(repository.countByModuleAndAction(AuditModule.TRANSACTION,AuditAction.TRANSFER_FAILED))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentAuditResponse> getRecentAudits(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return repository
                .findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(this::mapToRecentAudit)
                .toList();
    }

    private RecentAuditResponse mapToRecentAudit(AuditLog audit) {
        return RecentAuditResponse.builder()
                .username(audit.getUsername())
                .module(audit.getModule().name())
                .action(audit.getAction().name())
                .description(audit.getDescription())
                .createdAt(audit.getCreatedAt())
                .build();
    }
}