package com.bank.accs.service;

import com.bank.accs.dtos.TransactionLimitRequest;
import com.bank.accs.dtos.TransactionLimitResponse;
import com.bank.accs.kafka.KafkaEventPublisher;
import com.bank.accs.model.TransactionLimit;
import com.bank.accs.repository.TransactionLimitRepository;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionLimitService {

    private final TransactionLimitRepository repository;
    private final KafkaEventPublisher eventPublisher;

    public TransactionLimitResponse getLimit(String accountNumber) {
        TransactionLimit limit = repository.findByAccountNumber(accountNumber).orElseThrow();
        return map(limit);
    }

    public TransactionLimitResponse updateLimit(String accountNumber, TransactionLimitRequest request) {
        TransactionLimit limit = repository.findByAccountNumber(accountNumber).orElseThrow();
        limit.setPerTransactionLimit(request.getPerTransactionLimit());
        limit.setDailyLimit(request.getDailyLimit());
        limit.setMonthlyLimit(request.getMonthlyLimit());
        limit.setUpdatedAt(LocalDateTime.now());
        repository.save(limit);
        eventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .module("LIMIT")
                        .action("LIMIT_UPDATED")
                        .entityId(limit.getId())
                        .entityType("TRANSACTION_LIMIT")
                        .description("Updated limit for "+ accountNumber)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(limit);
    }

    private TransactionLimitResponse map(TransactionLimit limit) {
        return TransactionLimitResponse.builder()
                .accountNumber(limit.getAccountNumber())
                .perTransactionLimit(limit.getPerTransactionLimit())
                .dailyLimit(limit.getDailyLimit())
                .monthlyLimit(limit.getMonthlyLimit())
                .build();
    }
}
