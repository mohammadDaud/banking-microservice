package com.bank.us.service;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import com.bank.us.dtos.EventMetadata;
import com.bank.us.dtos.UserProfileRequest;
import com.bank.us.dtos.UserProfileResponse;
import com.bank.us.dtos.UserResponse;
import com.bank.us.enums.UserStatus;
import com.bank.us.kafka.KafkaEventPublisher;
import com.bank.us.model.UserProfile;
import com.bank.us.repository.UserProfileRepository;
import com.bank.us.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String SERVICE_NAME = "user-service";

    private final UserProfileRepository repository;
    private  final KafkaEventPublisher  eventPublisher;

    public UserProfile getProfile(String userId) {
        return repository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User profile not found : " + userId
                        ));
    }

    public UserProfile updateProfile(String userId,UserProfileRequest request,HttpServletRequest servletRequest) {
        if (request == null) {
            throw new RuntimeException("Profile request cannot be null");
        }
        UserProfile profile = getProfile(userId);
        updateProfileFields(profile, request);
        UserProfile savedProfile = repository.save(profile);
        log.info("Profile updated successfully for userId={}", savedProfile.getUserId());
        publishProfileAudit(savedProfile,"PROFILE_UPDATED","Customer profile updated successfully",servletRequest);
        publishNotification(savedProfile.getUserId(),"Profile Updated","Your profile has been updated successfully.","LOW",EventStatus.SUCCESS);
        return savedProfile;
    }


    public List<UserProfileResponse> getAllCustomers() {
        return repository.findAll().stream().map(this::map).toList();
    }
    public Long count() {
        return repository.count();
    }

    public Long countByStatus(String active) {
        return repository.countByStatus(active);
    }

    public List<UserResponse> findAllByOrderByCreatedAtDesc(PageRequest of) {
        Pageable pageable = PageRequest.of(of.getPageNumber(), of.getPageSize());
        List<UserProfile> userProfile= repository.findAllByOrderByCreatedAtDesc(pageable);
        return userProfile.stream()
                .map(this::mapUser)
                .toList();
    }


    private UserProfileResponse map(UserProfile profile) {
        return UserProfileResponse
                .builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .mobileNumber(profile.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .nationality(profile.getNationality())
                .maritalStatus(profile.getMaritalStatus())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private UserResponse mapUser(UserProfile profile) {
        return UserResponse
                .builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .email(profile.getEmail())
                .firstName(profile.getFirstName())
                .middleName(profile.getMiddleName())
                .lastName(profile.getLastName())
                .mobileNumber(profile.getMobileNumber())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .nationality(profile.getNationality())
                .maritalStatus(profile.getMaritalStatus())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }


    private void updateProfileFields(
            UserProfile profile,
            UserProfileRequest request) {
        profile.setFirstName(request.getFirstName());
        profile.setMiddleName(request.getMiddleName());
        profile.setLastName(request.getLastName());
        profile.setMobileNumber(request.getMobileNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationality(request.getNationality());
        profile.setMaritalStatus(request.getMaritalStatus());
        profile.setStatus(UserStatus.ACTIVE.name());
        profile.setUpdatedAt(LocalDateTime.now());
    }

    private EventMetadata createEventMetadata() {
        return EventMetadata.builder()
                .correlationId(CorrelationIdUtil.getCorrelationId())
                .requestId(EventMetadataUtil.requestId())
                .createdAt(EventMetadataUtil.createdAt())
                .build();
    }

    private void publishNotification(
            String userId,
            String title,
            String message,
            String priority,
            EventStatus status) {

        EventMetadata metadata = createEventMetadata();
        eventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(metadata.getCorrelationId())
                        .requestId(metadata.getRequestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.USER_SERVICE)
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type("PROFILE")
                        .priority(priority)
                        .status(status)
                        .createdAt(metadata.getCreatedAt())
                        .build()
        );
    }

    private void publishProfileAudit(
            UserProfile profile,
            String action,
            String description,
            HttpServletRequest request) {
        EventMetadata metadata = createEventMetadata();
        eventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(metadata.getCorrelationId())
                        .requestId(metadata.getRequestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.USER_SERVICE)
                        .requestUri(request != null
                                ? request.getRequestURI()
                                : null)
                        .requestMethod(request != null
                                ? request.getMethod()
                                : null)
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .role("ROLE_CUSTOMER")
                        .module("PROFILE")
                        .action(action)
                        .entityId(profile.getUserId())
                        .entityType("USER_PROFILE")
                        .description(description)
                        .status(EventStatus.SUCCESS)
                        .ipAddress(request != null
                                ? IpUtil.getClientIp(request)
                                : "SYSTEM")
                        .createdAt(metadata.getCreatedAt())
                        .build()
        );
    }

}