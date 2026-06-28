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

import java.time.LocalDate;
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
    public AuditDashboardResponse dashboard() {

        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return AuditDashboardResponse.builder()
                .totalLogs(repository.count())

                .todayLogins(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.LOGIN_SUCCESS,
                                start,
                                end))

                .todayTransfers(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.TRANSFER_SUCCESS,
                                start,
                                end))

                .todayFailedTransfers(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.TRANSFER_FAILED,
                                start,
                                end))

                .todayAccountsCreated(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.ACCOUNT_CREATED,
                                start,
                                end))

                .todayBeneficiariesAdded(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.BENEFICIARY_ADDED,
                                start,
                                end))

                .todayKycApproved(
                        repository.countByActionAndCreatedAtBetween(
                                AuditAction.KYC_APPROVED,
                                start,
                                end))

                .build();
    }
}