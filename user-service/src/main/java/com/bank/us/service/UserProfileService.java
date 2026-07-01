package com.bank.us.service;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.CustomerDeletedEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import com.bank.us.client.AccountClient;
import com.bank.us.dtos.*;
import com.bank.us.enums.UserStatus;
import com.bank.us.exception.BadRequestException;
import com.bank.us.exception.CustomerNotFoundException;
import com.bank.us.kafka.KafkaEventPublisher;
import com.bank.us.model.UserProfile;
import com.bank.us.repository.UserProfileRepository;
import com.bank.us.specification.UserProfileSpecification;
import com.bank.us.util.IpUtil;
import com.bank.us.validation.CustomerValidationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String SERVICE_NAME = "user-service";

    private final UserProfileRepository repository;
    private final KafkaEventPublisher eventPublisher;
    private final CustomerValidationService customerValidationService;
    private final AccountClient accountClient;


    public UserProfileResponse getProfile(String userId) {
        UserProfile profile = repository
                .findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(userId));
        return map(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(String userId, UserProfileRequest request, HttpServletRequest servletRequest) {
        if (request == null) {
            throw new BadRequestException("Profile request cannot be null.");
        }
        UserProfile profile = repository
                .findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(userId));
        updateProfileFields(profile, request,servletRequest);
        UserProfile savedProfile = repository.save(profile);
        log.info("Profile updated successfully for userId={}", savedProfile.getUserId());
        publishProfileAudit(savedProfile, "PROFILE_UPDATED", "Customer profile updated successfully", servletRequest);
        publishNotification(savedProfile.getUserId(), "Profile Updated", "Your profile has been updated successfully.", "LOW", EventStatus.SUCCESS);
        return map(savedProfile);
    }


    public List<UserProfileResponse> getAllCustomers() {
        return repository
                .findAllByDeletedFalse()
                .stream()
                .map(this::map)
                .toList();
    }

    public Long count() {
        return repository.countByDeletedFalse();
    }

    public Long countByStatus(UserStatus active) {
        return repository.countByStatusAndDeletedFalse(active);
    }

    public DashboardStatsResponse getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return DashboardStatsResponse
                .builder()
                .totalCustomers(repository.countByDeletedFalse())
                .activeCustomers(repository.countByStatusAndDeletedFalse(UserStatus.ACTIVE))
                .inactiveCustomers(repository.countByStatusAndDeletedFalse(UserStatus.INACTIVE))
                .registeredToday(repository.countByCreatedAtBetweenAndDeletedFalse(start, end))
                .build();
    }

    public List<UserResponse> findAllByOrderByCreatedAtDesc(PageRequest of) {
        Pageable pageable = PageRequest.of(of.getPageNumber(), of.getPageSize());
        List<UserProfile> userProfile = repository.findAllByDeletedFalseOrderByCreatedAtDesc(pageable);
        return userProfile.stream()
                .map(this::mapUser)
                .toList();
    }

    public PageResponse<UserProfileResponse> getCustomers(UserProfileSearchRequest request) {
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = request.getSortBy();

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        Pageable pageable = PageRequest.of(request.getPage(),
                request.getSize(),
                Sort.by(direction, sortBy));
        Specification<UserProfile> specification =
                Specification.allOf(
                        UserProfileSpecification.notDeleted(),
                        UserProfileSpecification.search(request.getSearch()),
                        UserProfileSpecification.status(request.getStatus()),
                        UserProfileSpecification.gender(request.getGender())
                );

        Page<UserProfile> customerPage = repository.findAll(specification, pageable);

        List<UserProfileResponse> customers = customerPage
                .getContent()
                .stream()
                .map(this::map)
                .toList();

        return PageResponse.<UserProfileResponse>builder()
                .content(customers)
                .page(customerPage.getNumber())
                .size(customerPage.getSize())
                .totalElements(customerPage.getTotalElements())
                .totalPages(customerPage.getTotalPages())
                .first(customerPage.isFirst())
                .last(customerPage.isLast())
                .empty(customerPage.isEmpty())
                .build();
    }

    @Transactional
    public ApiResponse deleteCustomer(String userId, HttpServletRequest request) {
        UserProfile profile = repository.findById(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId));
        if (Boolean.TRUE.equals(profile.getDeleted())) {
            throw new BadRequestException("Customer is already deleted.");
        }
        customerValidationService.validateCustomerDeletion(userId);
        profile.setDeleted(true);
        profile.setDeletedAt(LocalDateTime.now());
        profile.setDeletedBy(getLoggedInAdmin(request));
        repository.save(profile);
        publishCustomerDeletedEvent(profile);
        publishProfileAudit(
                profile,
                "CUSTOMER_DELETED",
                "Customer deleted successfully",
                request
        );

        publishNotification(
                profile.getUserId(),
                "Customer Deleted",
                "Your customer account has been deleted.",
                "HIGH",
                EventStatus.SUCCESS
        );

        return ApiResponse.builder()
                .success(true)
                .message("Customer deleted successfully.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void publishCustomerDeletedEvent(UserProfile profile) {
        log.info("Publishing CUSTOMER_DELETED event. customerId={}",profile.getUserId());
        List<String> accountNumbers = Collections.emptyList();
        try {
            accountNumbers = accountClient.getCustomerAccounts(profile.getUserId());
        } catch (Exception ex) {
            log.error("Unable to fetch account numbers for customerId={}",profile.getUserId(),ex);
        }
        eventPublisher.publish(
                KafkaTopics.CUSTOMER_DELETED_TOPIC,
                CustomerDeletedEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(CorrelationIdUtil.getCorrelationId())
                        .requestId(EventMetadataUtil.requestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.USER_SERVICE)
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .email(profile.getEmail())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .mobileNumber(profile.getMobileNumber())
                        .accountNumbers(accountNumbers)
                        .deletedAt(profile.getDeletedAt())
                        .deletedBy(profile.getDeletedBy())
                        .createdAt(EventMetadataUtil.createdAt())
                        .build()
        );
        log.info("CUSTOMER_DELETED event published successfully. customerId={}",profile.getUserId());
    }

    public UserProfileResponse getCustomer(String userId) {

        UserProfile profile = repository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(userId));
        return map(profile);
    }

    private String getLoggedInAdmin(HttpServletRequest request) {

        return request.getHeader("X-User-Id");

    }

    @Transactional
    public ApiResponse restoreCustomer(String userId, HttpServletRequest request) {

        UserProfile profile = repository.findById(userId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(userId));
        if (Boolean.FALSE.equals(profile.getDeleted())) {
            throw new BadRequestException("Customer is already active.");
        }
        profile.setDeleted(false);
        profile.setDeletedAt(null);
        profile.setDeletedBy(null);

        repository.save(profile);
        publishProfileAudit(
                profile,
                "CUSTOMER_RESTORED",
                "Customer restored successfully",
                request
        );
        publishNotification(
                profile.getUserId(),
                "Customer Restored",
                "Your customer account has been restored.",
                "LOW",
                EventStatus.SUCCESS
        );
        return ApiResponse.builder()
                .success(true)
                .message("Customer restored successfully.")
                .timestamp(LocalDateTime.now())
                .build();

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
                .status(profile.getStatus().name())
                .deleted(profile.getDeleted())
                .deletedAt(profile.getDeletedAt())
                .deletedBy(profile.getDeletedBy())
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
                .status(profile.getStatus().name())
                .deleted(profile.getDeleted())
                .deletedAt(profile.getDeletedAt())
                .deletedBy(profile.getDeletedBy())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }


    private void updateProfileFields(
            UserProfile profile,
            UserProfileRequest request,
            HttpServletRequest servletRequest){
        profile.setFirstName(request.getFirstName());
        profile.setMiddleName(request.getMiddleName());
        profile.setLastName(request.getLastName());
        profile.setMobileNumber(request.getMobileNumber());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setNationality(request.getNationality());
        profile.setMaritalStatus(request.getMaritalStatus());
        //profile.setStatus(UserStatus.ACTIVE);
        profile.setUpdatedAt(LocalDateTime.now());
        profile.setUpdatedBy(getLoggedInAdmin(servletRequest));
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