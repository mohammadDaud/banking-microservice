package com.bank.service.impl;

import com.bank.client.NotificationClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;
import com.bank.dtos.NotificationRequest;
import com.bank.enums.BeneficiaryStatus;
import com.bank.exception.BeneficiaryAlreadyExistsException;
import com.bank.exception.BeneficiaryNotFoundException;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Beneficiary;
import com.bank.repository.BeneficiaryRepository;
import com.bank.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository repository;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Override
    public BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request) {

        if(repository.existsByCustomerIdAndAccountNumber(request.getCustomerId(),request.getAccountNumber())) {
            throw new BeneficiaryAlreadyExistsException("Beneficiary already exists");
        }

        Beneficiary beneficiary =
                Beneficiary.builder()
                        .id(UUID.randomUUID().toString())
                        .customerId(request.getCustomerId())
                        .beneficiaryName(request.getBeneficiaryName())
                        .accountNumber(request.getAccountNumber())
                        .bankName(request.getBankName())
                        .ifscCode(request.getIfscCode())
                        .nickname(request.getNickname())
                        .status(BeneficiaryStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        repository.save(beneficiary);
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(beneficiary.getCustomerId())
                        .username(beneficiary.getBeneficiaryName())
                        .role("ROLE_CUSTOMER")
                        .module("BENEFICIARY")
                        .action("BENEFICIARY_ADDED")
                        .entityId(beneficiary.getId())
                        .entityType("BENEFICIARY")
                        .ipAddress("127.0.0.1")
                        .description("Beneficiary added successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Added")
                        .message(beneficiary.getBeneficiaryName()+ " added successfully")
                        .build()
        );
        return map(beneficiary);
    }

    @Override
    public List<BeneficiaryResponse> getCustomerBeneficiaries(String customerId) {
        return repository.findByCustomerId(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public BeneficiaryResponse approveBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary =
                repository.findById(beneficiaryId)
                        .orElseThrow(() ->
                                new BeneficiaryNotFoundException("Beneficiary not found"));

        if(beneficiary.getStatus()== BeneficiaryStatus.APPROVED) {
            throw new RuntimeException("Beneficiary already approved");
        }
        beneficiary.setStatus(BeneficiaryStatus.APPROVED);
        beneficiary.setUpdatedAt(LocalDateTime.now());
        repository.save(beneficiary);
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Approved")
                        .message(beneficiary.getBeneficiaryName()+ " approved")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(beneficiary.getCustomerId())
                        .username(beneficiary.getBeneficiaryName())
                        .role("ROLE_CUSTOMER")
                        .module("BENEFICIARY")
                        .action("BENEFICIARY_APPROVED")
                        .entityId(beneficiary.getId())
                        .entityType("BENEFICIARY")
                        .ipAddress("127.0.0.1")
                        .description("Beneficiary Approved successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(beneficiary);
    }

    @Override
    public BeneficiaryResponse rejectBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary =
                repository.findById(beneficiaryId)
                        .orElseThrow(() ->
                                new BeneficiaryNotFoundException("Beneficiary not found"));
        if(beneficiary.getStatus()== BeneficiaryStatus.REJECTED) {
            throw new RuntimeException("Beneficiary already rejected");
        }
        beneficiary.setStatus(BeneficiaryStatus.REJECTED);
        beneficiary.setUpdatedAt(LocalDateTime.now());
        repository.save(beneficiary);
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Rejected")
                        .message(beneficiary.getBeneficiaryName()+ " rejected")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(beneficiary.getCustomerId())
                        .username(beneficiary.getBeneficiaryName())
                        .role("ROLE_CUSTOMER")
                        .module("BENEFICIARY")
                        .action("BENEFICIARY_REJECTED")
                        .entityId(beneficiary.getId())
                        .entityType("BENEFICIARY")
                        .ipAddress("127.0.0.1")
                        .description("Beneficiary Rejected!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(beneficiary);
    }

    @Override
    public void deleteBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary =
                repository.findById(beneficiaryId)
                        .orElseThrow(() ->
                                new BeneficiaryNotFoundException("Beneficiary not found"));
        repository.delete(beneficiary);
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(beneficiary.getCustomerId())
                        .username(beneficiary.getBeneficiaryName())
                        .role("ROLE_CUSTOMER")
                        .module("BENEFICIARY")
                        .action("BENEFICIARY_REMOVED")
                        .entityId(beneficiary.getId())
                        .entityType("BENEFICIARY")
                        .ipAddress("127.0.0.1")
                        .description("Beneficiary Removed!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    @Override
    public Long getBeneficiaryCount(String customerId) {
        return repository.countByCustomerId(customerId);
    }

    @Override
    public List<BeneficiaryResponse> getPendingBeneficiaries() {
        return repository
                .findByStatus(BeneficiaryStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long count() {
        return repository.count();
    }

    private BeneficiaryResponse map(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .accountNumber(beneficiary.getAccountNumber())
                .bankName(beneficiary.getBankName())
                .ifscCode(beneficiary.getIfscCode())
                .nickname(beneficiary.getNickname())
                .status(beneficiary.getStatus().name())
                .build();
    }
}