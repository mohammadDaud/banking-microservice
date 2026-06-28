package com.bank.service.impl;

import com.bank.client.NotificationClient;
import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import com.bank.dtos.EventMetadata;
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
import com.bank.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private static final String SERVICE_NAME = "kyc-service";

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
            MultipartFile aadhaarDocument,
            HttpServletRequest httpServletRequest) {

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
        log.debug(
                "KYC CREATE Service : userId=" + userId
                        + ", pan=" + panNumber
                        + ", aadhaar=" + aadhaarNumber
                        + ", panFile=" + panDocument.getOriginalFilename()
                        + ", aadhaarFile=" + aadhaarDocument.getOriginalFilename()
        );

        sendNotification(
                savedProfile.getUserId(),
                "KYC Submitted",
                "Your KYC request has been submitted for approval"
        );

        publishAudit(
                savedProfile.getMakerId(),
                "ROLE_CUSTOMER",
                "KYC_SUBMITTED_FOR_APPROVAL",
                savedProfile,
                "KYC submitted for checker review",
                httpServletRequest
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
            MultipartFile aadhaarDocument,
            HttpServletRequest httpServletRequest) {

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

        sendNotification(
                savedProfile.getUserId(),
                "KYC Resubmitted",
                "Your corrected KYC documents were submitted for approval"
        );

        publishAudit(
                userId,
                "ROLE_CUSTOMER",
                "KYC_RESUBMITTED_FOR_APPROVAL",
                savedProfile,
                "KYC resubmitted after rejection",
                httpServletRequest
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
            String remark, HttpServletRequest httpServletRequest) {

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
        sendNotification(savedProfile.getUserId(), "KYC Under Review",
                "Your KYC is currently under review");

        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_MOVED_TO_UNDER_REVIEW",
                savedProfile,
                "KYC moved to UNDER_REVIEW by checker: " + checkerId,
                httpServletRequest
        );

        return map(savedProfile);
    }

    @Override
    @Transactional
    public KycResponse approveKyc(
            String userId,
            String checkerId,
            String remark,
            HttpServletRequest httpServletRequest) {

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

        sendNotification(
                savedProfile.getUserId(),
                "KYC Approved",
                "Your KYC has been approved successfully");

        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_APPROVED",
                savedProfile,
                "KYC approved by checker: " + checkerId,
                httpServletRequest
        );
        publishNotificationEvent(
                savedProfile.getUserId(),
                "KYC Approved",
                "Your KYC has been approved",
                "KYC",
                "HIGH",
                EventStatus.SUCCESS
        );

        return map(savedProfile);
    }

    @Override
    @Transactional
    public KycResponse rejectKyc(
            String userId,
            String checkerId,
            String remark,
            HttpServletRequest httpServletRequest) {

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


        sendNotification(
                savedProfile.getUserId(),
                "KYC Rejected",
                "Your KYC was rejected. Reason: "
                        + rejectionRemark
        );
        publishAudit(
                checkerId,
                "ROLE_CHECKER",
                "KYC_REJECTED",
                savedProfile,
                "KYC rejected by checker: " + checkerId
                        + ". Remark: " + rejectionRemark,
                httpServletRequest
        );
        publishNotificationEvent(
                savedProfile.getUserId(),
                "KYC Rejected",
                "Your KYC was rejected. Reason: "
                        + rejectionRemark,
                "KYC",
                "HIGH",
                EventStatus.SUCCESS
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
        boolean eligible = profile.getKycStatus() == KycStatus.APPROVED;
        return KycEligibilityResponse.builder()
                .userId(profile.getUserId())
                .eligible(eligible)
                .status(profile.getKycStatus().name())
                .message(eligible
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
                || !Objects.equals(profile.getCheckerId(),checkerId)) {

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
                        .source(EventSource.KYC_SERVICE)
                        .requestUri(request != null ? request.getRequestURI() : null)
                        .requestMethod(request != null ? request.getMethod() : null)
                        .userId(userId)
                        .username(profile.getUserId())
                        .role(role)
                        .module("KYC")
                        .action(action)
                        .entityId(profile.getId())
                        .entityType("KYC")
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

    private void sendNotification(
            String userId,
            String title,
            String message) {

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .build()
        );
    }

    private void publishNotificationEvent(
            String userId,
            String title,
            String message,
            String type,
            String priority,
            EventStatus status) {

        EventMetadata metadata = createEventMetadata();

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(metadata.getCorrelationId())
                        .requestId(metadata.getRequestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.KYC_SERVICE)
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type(type)
                        .priority(priority)
                        .status(status)
                        .createdAt(metadata.getCreatedAt())
                        .build()
        );
    }
}