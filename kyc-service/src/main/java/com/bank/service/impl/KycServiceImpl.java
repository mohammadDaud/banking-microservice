package com.bank.service.impl;

import com.bank.client.NotificationClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.KycApprovalRequest;
import com.bank.dtos.KycEligibilityResponse;
import com.bank.dtos.KycResponse;
import com.bank.dtos.NotificationRequest;
import com.bank.enums.KycStatus;
import com.bank.exception.KycAlreadyExistsException;
import com.bank.exception.KycNotFoundException;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.KycProfile;
import com.bank.repository.KycRepository;
import com.bank.service.FileStorageService;
import com.bank.service.KycService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final KycRepository repository;
    private final FileStorageService fileStorageService;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Override
    @Transactional
    public KycResponse createKyc(
            String userId,
            String panNumber,
            String aadhaarNumber,
            MultipartFile panDocument,
            MultipartFile aadhaarDocument) {

        if (repository.existsByUserId(userId)) {
            throw new KycAlreadyExistsException("KYC already submitted");
        }

        validateDocuments(panDocument, aadhaarDocument);

        String panPath = fileStorageService.store(panDocument);
        String aadhaarPath = fileStorageService.store(aadhaarDocument);

        LocalDateTime now = LocalDateTime.now();

        KycProfile profile = KycProfile.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .makerId(userId)
                .panNumber(panNumber)
                .aadhaarNumber(aadhaarNumber)
                .panDocumentPath(panPath)
                .aadhaarDocumentPath(aadhaarPath)
                .kycStatus(KycStatus.PENDING)
                .submittedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        KycProfile savedProfile = repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Submitted")
                        .message("Your KYC request has been submitted for approval")
                        .build()
        );

        publishAudit(
                savedProfile.getMakerId(),
                "ROLE_CUSTOMER",
                "KYC_SUBMITTED_FOR_APPROVAL",
                savedProfile,
                "KYC submitted for checker review"
        );

        return map(savedProfile);
    }

    @Override
    @Transactional
    public KycResponse resubmitKyc(
            String userId,
            String panNumber,
            String aadhaarNumber,
            MultipartFile panDocument,
            MultipartFile aadhaarDocument) {

        KycProfile profile = getProfile(userId);

        if (profile.getKycStatus() != KycStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only REJECTED KYC can be resubmitted"
            );
        }

        validateDocuments(panDocument, aadhaarDocument);

        String panPath = fileStorageService.store(panDocument);
        String aadhaarPath = fileStorageService.store(aadhaarDocument);

        LocalDateTime now = LocalDateTime.now();

        profile.setPanNumber(panNumber);
        profile.setAadhaarNumber(aadhaarNumber);
        profile.setPanDocumentPath(panPath);
        profile.setAadhaarDocumentPath(aadhaarPath);

        /*
         * New maker request:
         * clear previous checker decision and send it back to PENDING.
         */
        profile.setKycStatus(KycStatus.PENDING);
        profile.setMakerId(userId);
        profile.setCheckerId(null);
        profile.setCheckerRemark(null);
        profile.setRemarks(null);
        profile.setSubmittedAt(now);
        profile.setReviewedAt(null);
        profile.setApprovedAt(null);
        profile.setRejectedAt(null);
        profile.setUpdatedAt(now);

        KycProfile savedProfile = repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Resubmitted")
                        .message(
                                "Your corrected KYC documents were submitted for approval"
                        )
                        .build()
        );

        publishAudit(
                userId,
                "ROLE_CUSTOMER",
                "KYC_RESUBMITTED_FOR_APPROVAL",
                savedProfile,
                "KYC resubmitted after rejection"
        );

        return map(savedProfile);
    }

    @Override
    public KycResponse getKyc(String userId) {
        return map(getProfile(userId));
    }

    @Override
    @Transactional
    public KycResponse reviewKyc(
            String userId,
            String checkerId,
            String remark) {

        KycProfile profile = getProfile(userId);

        if (profile.getKycStatus() != KycStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING KYC can move to UNDER_REVIEW"
            );
        }

        validateMakerChecker(profile, checkerId);

        LocalDateTime now = LocalDateTime.now();

        profile.setKycStatus(KycStatus.UNDER_REVIEW);
        profile.setCheckerId(checkerId);
        profile.setCheckerRemark(
                normalizeOptionalRemark(
                        remark,
                        "KYC moved to under review"
                )
        );
        profile.setReviewedAt(now);
        profile.setUpdatedAt(now);

        KycProfile savedProfile = repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Under Review")
                        .message("Your KYC is currently under review")
                        .build()
        );

        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_MOVED_TO_UNDER_REVIEW",
                savedProfile,
                "KYC moved to UNDER_REVIEW by checker: " + checkerId
        );

        return map(savedProfile);
    }

    @Override
    @Transactional
    public KycResponse approveKyc(
            String userId,
            String checkerId,
            String remark) {

        KycProfile profile = getProfile(userId);

        if (profile.getKycStatus() != KycStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only UNDER_REVIEW KYC can be approved"
            );
        }

        validateMakerChecker(profile, checkerId);
        validateAssignedChecker(profile, checkerId);

        LocalDateTime now = LocalDateTime.now();

        profile.setKycStatus(KycStatus.APPROVED);
        profile.setCheckerRemark(
                normalizeOptionalRemark(
                        remark,
                        "KYC approved by checker"
                )
        );
        profile.setApprovedAt(now);
        profile.setUpdatedAt(now);

        KycProfile savedProfile = repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Approved")
                        .message("Your KYC has been approved successfully")
                        .build()
        );

        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_APPROVED",
                savedProfile,
                "KYC approved by checker: " + checkerId
        );

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Approved")
                        .message("Your KYC has been approved")
                        .type("KYC")
                        .priority("HIGH")
                        .build()
        );

        return map(savedProfile);
    }

    @Override
    @Transactional
    public KycResponse rejectKyc(
            String userId,
            String checkerId,
            String remark) {

        KycProfile profile = getProfile(userId);

        if (profile.getKycStatus() != KycStatus.UNDER_REVIEW) {
            throw new IllegalStateException(
                    "Only UNDER_REVIEW KYC can be rejected"
            );
        }

        validateMakerChecker(profile, checkerId);
        validateAssignedChecker(profile, checkerId);

        if (remark == null || remark.isBlank()) {
            throw new IllegalArgumentException(
                    "Remark is required when rejecting KYC"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String rejectionRemark = remark.trim();

        profile.setKycStatus(KycStatus.REJECTED);
        profile.setRemarks(rejectionRemark);
        profile.setCheckerRemark(rejectionRemark);
        profile.setRejectedAt(now);
        profile.setUpdatedAt(now);

        KycProfile savedProfile = repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Rejected")
                        .message(
                                "Your KYC was rejected. Reason: "
                                        + rejectionRemark
                        )
                        .build()
        );

        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_REJECTED",
                savedProfile,
                "KYC rejected by checker: " + checkerId
                        + ". Remark: " + rejectionRemark
        );

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(savedProfile.getUserId())
                        .title("KYC Rejected")
                        .message(
                                "Your KYC was rejected. Reason: "
                                        + rejectionRemark
                        )
                        .type("KYC")
                        .priority("HIGH")
                        .build()
        );

        return map(savedProfile);
    }

    @Override
    public List<KycResponse> getPendingKyc() {
        return repository.findByKycStatus(KycStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long countByStatus(String status) {
        return repository.countByKycStatus(status);
    }

    @Override
    public List<Object[]> getStats() {
        return repository.getStats();
    }

    @Override
    public KycEligibilityResponse checkEligibility(String userId) {

        KycProfile profile = getProfile(userId);

        boolean eligible =
                profile.getKycStatus() == KycStatus.APPROVED;

        return KycEligibilityResponse.builder()
                .userId(profile.getUserId())
                .eligible(eligible)
                .status(profile.getKycStatus().name())
                .message(
                        eligible
                                ? "Customer KYC is approved"
                                : "Customer KYC is "
                                  + profile.getKycStatus()
                                  + ". Banking operation requires APPROVED KYC"
                )
                .build();
    }

    @Override
    public List<KycResponse> getKycByStatus(String status) {
        KycStatus kycStatus;

        try {
            kycStatus = KycStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid KYC status. Allowed values: "
                            + "PENDING, UNDER_REVIEW, APPROVED, REJECTED"
            );
        }

        return repository.findByKycStatus(kycStatus)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<KycResponse> getCheckerQueue() {
        return repository.findByKycStatusIn(
                        List.of(
                                KycStatus.PENDING,
                                KycStatus.UNDER_REVIEW
                        )
                )
                .stream()
                .map(this::map)
                .toList();
    }

    private KycProfile getProfile(String userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() ->
                        new KycNotFoundException(
                                "KYC not found for user: " + userId
                        )
                );
    }

    private void validateMakerChecker(
            KycProfile profile,
            String checkerId) {

        if (checkerId == null || checkerId.isBlank()) {
            throw new IllegalArgumentException("Checker ID is required");
        }

        if (checkerId.equals(profile.getMakerId())) {
            throw new IllegalStateException(
                    "Maker cannot review, approve, or reject their own KYC request"
            );
        }
    }

    private void validateAssignedChecker(
            KycProfile profile,
            String checkerId) {

        if (profile.getCheckerId() == null
                || !profile.getCheckerId().equals(checkerId)) {

            throw new IllegalStateException(
                    "Only the checker assigned during review "
                            + "can approve or reject this KYC"
            );
        }
    }

    private void validateDocuments(
            MultipartFile panDocument,
            MultipartFile aadhaarDocument) {

        if (panDocument == null || panDocument.isEmpty()) {
            throw new IllegalArgumentException("PAN document is required");
        }

        if (aadhaarDocument == null || aadhaarDocument.isEmpty()) {
            throw new IllegalArgumentException("Aadhaar document is required");
        }
    }

    private String normalizeOptionalRemark(
            String remark,
            String defaultRemark) {

        if (remark == null || remark.isBlank()) {
            return defaultRemark;
        }

        return remark.trim();
    }

    private KycResponse map(KycProfile profile) {
        return KycResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .panNumber(profile.getPanNumber())
                .aadhaarNumber(profile.getAadhaarNumber())
                .panDocumentUrl(profile.getPanDocumentPath())
                .aadhaarDocumentUrl(profile.getAadhaarDocumentPath())
                .kycStatus(profile.getKycStatus())
                .remarks(profile.getRemarks())
                .makerId(profile.getMakerId())
                .checkerId(profile.getCheckerId())
                .checkerRemark(profile.getCheckerRemark())
                .submittedAt(profile.getSubmittedAt())
                .reviewedAt(profile.getReviewedAt())
                .approvedAt(profile.getApprovedAt())
                .rejectedAt(profile.getRejectedAt())
                .build();
    }

    private void publishAudit(
            String userId,
            String role,
            String action,
            KycProfile profile,
            String description) {

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(userId)
                        .username(null)
                        .role(role)
                        .module("KYC")
                        .action(action)
                        .entityId(profile.getId())
                        .entityType("KYC")
                        .ipAddress("127.0.0.1")
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}