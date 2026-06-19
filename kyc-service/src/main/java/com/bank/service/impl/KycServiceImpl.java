package com.bank.service.impl;

import com.bank.client.NotificationClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.CreateKycRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
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
    public KycResponse createKyc(
            String userId,
            String panNumber,
            String aadhaarNumber,
            MultipartFile panDocument,
            MultipartFile aadhaarDocument) {

        if(repository.existsByUserId(userId)) {
            throw new KycAlreadyExistsException("KYC already submitted");
        }

        String panPath =
                fileStorageService.store(panDocument);
        String aadhaarPath =
                fileStorageService.store(aadhaarDocument);

        KycProfile profile =
                KycProfile.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .panNumber(panNumber)
                        .aadhaarNumber(aadhaarNumber)
                        .panDocumentPath(panPath)
                        .aadhaarDocumentPath(aadhaarPath)
                        .kycStatus(KycStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
        repository.save(profile);

        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(profile.getUserId())
                        .title("KYC Submitted")
                        .message("Your KYC request submitted")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(profile.getUserId())
                        .username(null)
                        .role("ROLE_CUSTOMER")
                        .module("KYC")
                        .action("KYC_SUBMITTED")
                        .entityId(profile.getId())
                        .entityType("KYC")
                        .ipAddress("127.0.0.1")
                        .description("Your KYC request submitted!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(profile);
    }

    @Override
    public KycResponse getKyc(String userId) {
        KycProfile profile =
                repository
                        .findByUserId(userId)
                        .orElseThrow(() -> new KycNotFoundException("KYC not found"));
        return map(profile);
    }

    @Override
    public KycResponse approveKyc(String userId) {
        KycProfile profile =
                repository
                        .findByUserId(userId)
                        .orElseThrow(() -> new KycNotFoundException("KYC not found"));
        validateKycAction(profile);
        if(profile.getKycStatus()== KycStatus.PENDING) {
            throw new KycAlreadyExistsException("Move KYC to UNDER_REVIEW before approval");
        }
        profile.setKycStatus(KycStatus.APPROVED);
        profile.setUpdatedAt(LocalDateTime.now());
        repository.save(profile);
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(profile.getUserId())
                        .title("KYC Approved")
                        .message("Your KYC approved successfully")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(profile.getUserId())
                        .username(null)
                        .role("ROLE_CUSTOMER")
                        .module("KYC")
                        .action("KYC_APPROVED")
                        .entityId(profile.getId())
                        .entityType("KYC")
                        .ipAddress("127.0.0.1")
                        .description("Your KYC approved successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(profile.getUserId())
                        .title("KYC Approved")
                        .message("Your KYC has been approved")
                        .type("KYC")
                        .priority("HIGH")
                        .build()
        );
        return map(profile);
    }

    @Override
    public KycResponse rejectKyc(String userId,String remarks) {
        KycProfile profile =
                repository
                        .findByUserId(userId)
                        .orElseThrow(() -> new KycNotFoundException("KYC not found"));
        validateKycAction(profile);
        if(profile.getKycStatus()== KycStatus.PENDING) {
            throw new KycAlreadyExistsException("Move KYC to UNDER_REVIEW before rejection");
        }
        profile.setKycStatus(KycStatus.REJECTED);
        profile.setRemarks(remarks);
        profile.setUpdatedAt(LocalDateTime.now());
        repository.save(profile);
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(profile.getUserId())
                        .title("KYC Rejected")
                        .message("Your KYC request rejected")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(profile.getUserId())
                        .username(null)
                        .role("ROLE_CUSTOMER")
                        .module("KYC")
                        .action("KYC_REJECTED")
                        .entityId(profile.getId())
                        .entityType("KYC")
                        .ipAddress("127.0.0.1")
                        .description("Your KYC request rejected!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(profile);
    }

    @Override
    public KycResponse reviewKyc(String userId) {
        KycProfile profile =
                repository
                        .findByUserId(userId)
                        .orElseThrow(
                                () -> new KycNotFoundException("KYC not found"));
        validateKycAction(profile);
        if(profile.getKycStatus() == KycStatus.UNDER_REVIEW) {
            throw new KycAlreadyExistsException("KYC already under review");
        }
        profile.setKycStatus(KycStatus.UNDER_REVIEW);
        profile.setUpdatedAt(LocalDateTime.now());
        repository.save(profile);
        return map(profile);
    }

    @Override
    public List<KycResponse> getPendingKyc() {
        return repository
                .findByKycStatus(KycStatus.PENDING)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long countByStatus(String pending) {
        return repository.countBykycStatus(pending);
    }

    @Override
    public List<Object[]> getStats() {
        return repository.getStats();
    }

    private KycResponse map(KycProfile profile) {
        return KycResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .panNumber(profile.getPanNumber())
                .aadhaarNumber(profile.getAadhaarNumber())
                .kycStatus(profile.getKycStatus())
                .remarks(profile.getRemarks())
                .build();
    }
    private void validateKycAction(KycProfile profile) {
        if (profile.getKycStatus()== KycStatus.APPROVED) {
            throw new KycAlreadyExistsException("KYC already approved");
        }
        if (profile.getKycStatus() == KycStatus.REJECTED) {
            throw new KycAlreadyExistsException("KYC already rejected");
        }
    }
}