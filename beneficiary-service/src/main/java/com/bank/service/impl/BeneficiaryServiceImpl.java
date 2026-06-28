package com.bank.service.impl;

import com.bank.client.KycClient;
import com.bank.client.NotificationClient;
import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import com.bank.dtos.*;
import com.bank.enums.BeneficiaryStatus;
import com.bank.exception.BeneficiaryAlreadyExistsException;
import com.bank.exception.BeneficiaryNotFoundException;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Beneficiary;
import com.bank.repository.BeneficiaryRepository;
import com.bank.service.BeneficiaryService;
import com.bank.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private static final String SERVICE_NAME = "beneficiary-service";
    public static final String CUSTOMER = "ROLE_CUSTOMER";
    public static final String CHECKER = "ROLE_CHECKER";

    private final BeneficiaryRepository repository;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final KycClient kycClient;

    @Override
    @Transactional
    public BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request, String makerId,HttpServletRequest httpServletRequest) {

        if (repository.existsByCustomerIdAndAccountNumber(
                request.getCustomerId(), request.getAccountNumber())) {
            throw new BeneficiaryAlreadyExistsException("Beneficiary already exists");
        }

        KycEligibilityResponse kycEligibility =
                kycClient.checkEligibility(request.getCustomerId());

        if (!kycEligibility.isEligible()) {
            throw new IllegalStateException("Beneficiary creation blocked: "
                    + kycEligibility.getMessage()
            );
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
                .makerId(makerId)
                .submittedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Beneficiary savedBeneficiary = repository.save(beneficiary);

        publishAudit(
                makerId,
                savedBeneficiary.getBeneficiaryName(),
                CUSTOMER,
                "BENEFICIARY_SUBMITTED_FOR_APPROVAL",
                savedBeneficiary.getId(),
                "Beneficiary submitted for checker approval",
                httpServletRequest
        );
        sendNotification(
                savedBeneficiary.getCustomerId(),
                "Beneficiary Pending Approval",
                savedBeneficiary.getBeneficiaryName()
                        + " has been submitted for approval"
        );


        return map(savedBeneficiary);
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
    public BeneficiaryResponse approveBeneficiary(String beneficiaryId, String checkerId, String remarks,HttpServletRequest httpServletRequest) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);

        if (beneficiary.getStatus() != BeneficiaryStatus.PENDING) {
            throw new IllegalStateException("Only pending beneficiary can be approved");
        }

        validateMakerChecker(beneficiary, checkerId);

        LocalDateTime now = LocalDateTime.now();

        beneficiary.setStatus(BeneficiaryStatus.APPROVED);
        beneficiary.setCheckerId(checkerId);
        beneficiary.setCheckerRemark(remarks == null || remarks.isBlank()
                ? "Approved by checker" : remarks.trim()
        );
        beneficiary.setApprovedAt(now);
        beneficiary.setUpdatedAt(now);

        Beneficiary approvedBeneficiary = repository.save(beneficiary);

        sendNotification(
                approvedBeneficiary.getCustomerId(),
                "Beneficiary Approved",
                approvedBeneficiary.getBeneficiaryName()
                        + " has been approved and can now be used for transfers"
        );

        publishAudit(
                checkerId,
                approvedBeneficiary.getBeneficiaryName(),
                CHECKER,
                "BENEFICIARY_APPROVED_BY_CHECKER",
                approvedBeneficiary.getId(),
                "Beneficiary approved by checker: " + checkerId,
                httpServletRequest
        );

        return map(approvedBeneficiary);
    }

    @Override
    @Transactional
    public BeneficiaryResponse rejectBeneficiary(String beneficiaryId, String checkerId, String remarks,HttpServletRequest httpServletRequest) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);
        if (beneficiary.getStatus() != BeneficiaryStatus.PENDING) {
            throw new IllegalStateException("Only pending beneficiary can be rejected");
        }

        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Remarks are required when rejecting a beneficiary");
        }

        validateMakerChecker(beneficiary, checkerId);

        LocalDateTime now = LocalDateTime.now();

        beneficiary.setStatus(BeneficiaryStatus.REJECTED);
        beneficiary.setCheckerId(checkerId);
        beneficiary.setCheckerRemark(remarks.trim());
        beneficiary.setRejectedAt(now);
        beneficiary.setUpdatedAt(now);

        Beneficiary rejectedBeneficiary = repository.save(beneficiary);

        sendNotification(
                rejectedBeneficiary.getCustomerId(),
                "Beneficiary Rejected",
                rejectedBeneficiary.getBeneficiaryName()
                        + " was rejected. Reason: " + remarks.trim()
        );

        publishAudit(
                checkerId,
                rejectedBeneficiary.getBeneficiaryName(),
                CHECKER,
                "BENEFICIARY_REJECTED_BY_CHECKER",
                rejectedBeneficiary.getId(),
                "Beneficiary rejected by checker: " + checkerId
                        + ". Remarks: " + remarks.trim(),
                httpServletRequest
        );

        return map(rejectedBeneficiary);
    }

    @Override
    @Transactional
    public void deleteBeneficiary(String beneficiaryId, String customerId,HttpServletRequest  httpServletRequest) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId);

        if (!beneficiary.getCustomerId().equals(customerId)) {
            throw new IllegalStateException(
                    "You can delete only your own beneficiary"
            );
        }

        if (beneficiary.getStatus() == BeneficiaryStatus.APPROVED) {
            throw new IllegalStateException(
                    "Approved beneficiary cannot be deleted. Contact support if required."
            );
        }

        repository.delete(beneficiary);

        publishAudit(
                customerId,
                beneficiary.getBeneficiaryName(),
                CUSTOMER,
                "BENEFICIARY_REMOVED",
                beneficiary.getId(),
                "Beneficiary removed successfully",
                httpServletRequest
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
    public BeneficiaryEligibilityResponse checkEligibility(
            String beneficiaryId,
            String customerId) {

        Beneficiary beneficiary = getBeneficiary(beneficiaryId);

        if (!beneficiary.getCustomerId().equals(customerId)) {
            throw new IllegalStateException(
                    "This beneficiary does not belong to the logged-in customer"
            );
        }

        boolean eligible =
                beneficiary.getStatus() == BeneficiaryStatus.APPROVED;

        return BeneficiaryEligibilityResponse.builder()
                .beneficiaryId(beneficiary.getId())
                .customerId(beneficiary.getCustomerId())
                .accountNumber(beneficiary.getAccountNumber())
                .bankName(beneficiary.getBankName())
                .ifscCode(beneficiary.getIfscCode())
                .eligible(eligible)
                .status(beneficiary.getStatus().name())
                .message(eligible
                        ? "Beneficiary is eligible for transaction"
                        : "Beneficiary must be approved before transaction")
                .build();
    }

    private Beneficiary getBeneficiary(String beneficiaryId) {
        return repository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryNotFoundException("Beneficiary not found"));
    }

    private void validateMakerChecker(Beneficiary beneficiary, String checkerId) {
        if (checkerId == null || checkerId.isBlank()) {
            throw new IllegalArgumentException("Checker ID is required");
        }
        if (Objects.equals(beneficiary.getMakerId(), checkerId)) {
            throw new IllegalStateException("Maker cannot approve or reject their own beneficiary request");
        }
    }

    private void sendNotification(
            String userId,
            String title,
            String message) {
        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type("BENEFICIARY")
                        .priority("HIGH")
                        .build());
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
                .checkerRemarks(beneficiary.getCheckerRemark())
                .submittedAt(beneficiary.getSubmittedAt())
                .approvedAt(beneficiary.getApprovedAt())
                .rejectedAt(beneficiary.getRejectedAt())
                .createdAt(beneficiary.getCreatedAt())
                .updatedAt(beneficiary.getUpdatedAt())
                .build();
    }

    private void publishAudit(
            String userId,
            String username,
            String role,
            String action,
            String entityId,
            String description,
            HttpServletRequest request) {

        EventMetadata metadata = createEventMetadata();

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(metadata.getCorrelationId())
                        .requestId(metadata.getRequestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.BENEFICIARY_SERVICE)

                        .requestUri(request != null ? request.getRequestURI() : null)
                        .requestMethod(request != null ? request.getMethod() : null)

                        .userId(userId)
                        .username(username)
                        .role(role)

                        .module("BENEFICIARY")
                        .action(action)
                        .entityId(entityId)
                        .entityType("BENEFICIARY")

                        .description(description)

                        .status(EventStatus.SUCCESS)

                        .ipAddress(request != null
                                ? IpUtil.getClientIp(request)
                                : "SYSTEM")

                        .createdAt(metadata.getCreatedAt())
                        .build()
        );
    }

    private EventMetadata createEventMetadata() {

        return EventMetadata.builder()
                .correlationId(CorrelationIdUtil.getCorrelationId())
                .requestId(EventMetadataUtil.requestId())
                .createdAt(EventMetadataUtil.createdAt())
                .build();
    }
}