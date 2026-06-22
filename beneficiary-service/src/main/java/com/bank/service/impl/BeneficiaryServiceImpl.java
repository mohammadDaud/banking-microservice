package com.bank.service.impl;

import com.bank.client.KycClient;
import com.bank.client.NotificationClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.*;
import com.bank.enums.BeneficiaryStatus;
import com.bank.exception.BeneficiaryAlreadyExistsException;
import com.bank.exception.BeneficiaryNotFoundException;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Beneficiary;
import com.bank.repository.BeneficiaryRepository;
import com.bank.service.BeneficiaryService;
import jakarta.transaction.Transactional;
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
    private final KycClient kycClient;

    @Override
    @Transactional
    public BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request) {

        if (repository.existsByCustomerIdAndAccountNumber(request.getCustomerId(),request.getAccountNumber())) {
            throw new BeneficiaryAlreadyExistsException("Beneficiary already exists");
        }

        // Kyc must be APPROVED of this customer.
        KycEligibilityResponse kycEligibility = kycClient.checkEligibility(request.getCustomerId());
        if (!kycEligibility.isEligible()) {
            throw new RuntimeException("Beneficiary Creation blocked: " + kycEligibility.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Beneficiary beneficiary = Beneficiary.builder()
                .id(UUID.randomUUID().toString())
                .customerId(request.getCustomerId())
                .beneficiaryName(request.getBeneficiaryName())
                .accountNumber(request.getAccountNumber())
                .bankName(request.getBankName())
                .ifscCode(request.getIfscCode())
                .nickname(request.getNickname())
                .status(BeneficiaryStatus.PENDING)
                .makerId(request.getMakerId())
                .submittedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        repository.save(beneficiary);

        publishAudit(
                beneficiary.getMakerId(),
                beneficiary.getBeneficiaryName(),
                "ROLE_MAKER",
                "BENEFICIARY_SUBMITTED_FOR_APPROVAL",
                beneficiary.getId(),
                "Beneficiary submitted for checker approval"
        );

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Pending Approval")
                        .message(beneficiary.getBeneficiaryName()
                                + " has been submitted for approval")
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
    @Transactional
    public BeneficiaryResponse approveBeneficiary(String beneficiaryId,BeneficiaryApprovalRequest request) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);
        if (beneficiary.getStatus() != BeneficiaryStatus.PENDING) {
            throw new IllegalStateException("Only pending beneficiary can be approved"
            );
        }

        validateMakerChecker(beneficiary, request.getCheckerId());

        LocalDateTime now = LocalDateTime.now();

        beneficiary.setStatus(BeneficiaryStatus.APPROVED);
        beneficiary.setCheckerId(request.getCheckerId());
        beneficiary.setCheckerRemark(request.getRemark());
        beneficiary.setApprovedAt(now);
        beneficiary.setUpdatedAt(now);

        repository.save(beneficiary);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Approved")
                        .message(beneficiary.getBeneficiaryName()
                                + " has been approved and can now be used for transfers")
                        .build()
        );

        publishAudit(
                request.getCheckerId(),
                beneficiary.getBeneficiaryName(),
                "ROLE_CHECKER",
                "BENEFICIARY_APPROVED",
                beneficiary.getId(),
                "Beneficiary approved by checker. Remark: " + request.getRemark()
        );

        return map(beneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryResponse rejectBeneficiary(String beneficiaryId,BeneficiaryApprovalRequest request) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);
        if (beneficiary.getStatus() != BeneficiaryStatus.PENDING) {
            throw new IllegalStateException("Only pending beneficiary can be rejected");
        }
        validateMakerChecker(beneficiary, request.getCheckerId());
        LocalDateTime now = LocalDateTime.now();
        beneficiary.setStatus(BeneficiaryStatus.REJECTED);
        beneficiary.setCheckerId(request.getCheckerId());
        beneficiary.setCheckerRemark(request.getRemark());
        beneficiary.setRejectedAt(now);
        beneficiary.setUpdatedAt(now);
        repository.save(beneficiary);
        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(beneficiary.getCustomerId())
                        .title("Beneficiary Rejected")
                        .message(beneficiary.getBeneficiaryName()
                                + " was rejected. Reason: " + request.getRemark())
                        .build()
        );

        publishAudit(
                request.getCheckerId(),
                beneficiary.getBeneficiaryName(),
                "ROLE_CHECKER",
                "BENEFICIARY_REJECTED",
                beneficiary.getId(),
                "Beneficiary rejected by checker. Remark: " + request.getRemark()
        );

        return map(beneficiary);
    }

    @Override
    @Transactional
    public void deleteBeneficiary(String beneficiaryId) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);
        repository.delete(beneficiary);

        publishAudit(
                beneficiary.getCustomerId(),
                beneficiary.getBeneficiaryName(),
                "ROLE_CUSTOMER",
                "BENEFICIARY_REMOVED",
                beneficiary.getId(),
                "Beneficiary removed successfully"
        );
    }

    @Override
    public Long getBeneficiaryCount(String customerId) {
        return repository.countByCustomerId(customerId);
    }

    @Override
    public List<BeneficiaryResponse> getPendingBeneficiaries() {
        return repository.findByStatus(BeneficiaryStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long count() {
        return repository.count();
    }

    @Override
    public BeneficiaryEligibilityResponse checkEligibility(String beneficiaryId,String customerId) {

        Beneficiary beneficiary = repository.findById(beneficiaryId)
                .orElseThrow(() ->
                        new BeneficiaryNotFoundException("Beneficiary not found"));

        if (!beneficiary.getCustomerId().equals(customerId)) {
            throw new IllegalStateException("Beneficiary does not belong to this customer");
        }

        boolean eligible = beneficiary.getStatus() == BeneficiaryStatus.APPROVED;

        return BeneficiaryEligibilityResponse.builder()
                .beneficiaryId(beneficiary.getId())
                .customerId(beneficiary.getCustomerId())
                .eligible(eligible)
                .status(beneficiary.getStatus().name())
                .message(eligible? "Beneficiary is eligible for transaction"
                        : "Beneficiary must be approved before transaction")
                .build();
    }

    private Beneficiary getBeneficiary(String beneficiaryId) {
        return repository.findById(beneficiaryId)
                .orElseThrow(() ->
                        new BeneficiaryNotFoundException("Beneficiary not found"));
    }

    private void validateMakerChecker(Beneficiary beneficiary,String checkerId) {
        if (beneficiary.getMakerId().equals(checkerId)) {
            throw new IllegalStateException("Maker cannot approve or reject their own beneficiary request"
            );
        }
    }

    private void publishAudit(
            String userId,
            String username,
            String role,
            String action,
            String entityId,
            String description) {

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(userId)
                        .username(username)
                        .role(role)
                        .module("BENEFICIARY")
                        .action(action)
                        .entityId(entityId)
                        .entityType("BENEFICIARY")
                        .ipAddress("127.0.0.1")
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private BeneficiaryResponse map(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .customerId(beneficiary.getCustomerId())
                .beneficiaryName(beneficiary.getBeneficiaryName())
                .accountNumber(beneficiary.getAccountNumber())
                .bankName(beneficiary.getBankName())
                .ifscCode(beneficiary.getIfscCode())
                .nickname(beneficiary.getNickname())
                .status(beneficiary.getStatus().name())
                .makerId(beneficiary.getMakerId())
                .checkerId(beneficiary.getCheckerId())
                .checkerRemark(beneficiary.getCheckerRemark())
                .submittedAt(beneficiary.getSubmittedAt())
                .approvedAt(beneficiary.getApprovedAt())
                .rejectedAt(beneficiary.getRejectedAt())
                .createdAt(beneficiary.getCreatedAt())
                .updatedAt(beneficiary.getUpdatedAt())
                .build();
    }
}