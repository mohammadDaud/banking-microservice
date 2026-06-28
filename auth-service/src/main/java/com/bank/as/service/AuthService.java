package com.bank.as.service;

import com.bank.as.kafka.KafkaEventPublisher;
import com.bank.as.model.dtos.EventMetadata;
import com.bank.as.model.dtos.RegisterRequest;
import com.bank.as.model.entites.EmailVerificationToken;
import com.bank.as.model.entites.PasswordHistory;
import com.bank.as.model.entites.Role;
import com.bank.as.model.entites.User;
import com.bank.as.repository.EmailVerificationTokenRepository;
import com.bank.as.repository.PasswordHistoryRepository;
import com.bank.as.repository.RoleRepository;
import com.bank.as.repository.UserRepository;
import com.bank.as.utill.IpUtil;
import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.EmailNotificationEvent;
import com.bank.common.events.UserRegisteredEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private static final String SERVICE_NAME = "auth-service";

    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {

        validateRegistration(request);

        User savedUser = createUser(request);

        String verificationToken = createEmailVerificationToken(savedUser);

        String correlationId = CorrelationIdUtil.getCorrelationId();
        String requestId = EventMetadataUtil.requestId();
        LocalDateTime createdAt = EventMetadataUtil.createdAt();

        publishUserRegisteredEvent(savedUser, correlationId, requestId, createdAt);

        publishEmailVerificationEvent(savedUser, verificationToken, correlationId, requestId, createdAt);

        savePasswordHistory(savedUser, createdAt);

        publishAuditEvent(savedUser, httpRequest, correlationId, requestId, createdAt);
    }

    @Transactional
    public void verifyEmail(String token, HttpServletRequest httpRequest) {
        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository.findByToken(token).orElseThrow(() ->
                        new RuntimeException("Invalid token"));

        if (verificationToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token expired");
        }

        User user = userRepository.findById(verificationToken.getUserId()).orElseThrow();

        user.setEmailVerified(true);

        userRepository.save(user);

        publishAudit(
                user,
                "AUTH",
                "EMAIL_VERIFIED",
                "Email verified successfully",
                httpRequest
        );

        verificationToken.setVerified(true);
    }

    private void validateRegistration(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
    }

    private User createUser(RegisterRequest request) {
        Role role = roleRepository.findByRoleName("ROLE_CUSTOMER").orElseThrow();
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(Set.of(role))
                .build();
        return userRepository.save(user);
    }

    private String createEmailVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        emailVerificationTokenRepository.save(
                EmailVerificationToken.builder()
                        .token(token)
                        .userId(user.getId())
                        .expiryTime(LocalDateTime.now().plusHours(24))
                        .verified(false)
                        .build());
        return token;
    }

    private void savePasswordHistory(User user, LocalDateTime createdAt) {
        passwordHistoryRepository.save(
                PasswordHistory.builder()
                        .userId(user.getId())
                        .passwordHash(user.getPassword())
                        .createdAt(createdAt)
                        .build());
    }

    private void publishUserRegisteredEvent(User user, String correlationId, String requestId, LocalDateTime createdAt) {
        kafkaEventPublisher.publish(
                KafkaTopics.USER_REGISTRATION_TOPIC,
                UserRegisteredEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(correlationId)
                        .requestId(requestId)
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.AUTH_SERVICE)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .status(EventStatus.SUCCESS)
                        .createdAt(createdAt)
                        .build());
    }

    private void publishEmailVerificationEvent(User user, String verificationToken, String correlationId, String requestId, LocalDateTime createdAt) {
        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(correlationId)
                        .requestId(requestId)
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.AUTH_SERVICE)
                        .to(user.getEmail())
                        .subject("Verify Your Email")
                        .body("Click the link below to verify your email:\n\n"
                                + "http://localhost:8081/api/auth/verify-email?token="
                                + verificationToken)
                        .templateName("EMAIL_VERIFICATION")
                        .status(EventStatus.PENDING)
                        .createdAt(createdAt)
                        .build());
    }

    private void publishAuditEvent(User user, HttpServletRequest request, String correlationId, String requestId, LocalDateTime createdAt) {
        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(correlationId)
                        .requestId(requestId)
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.AUTH_SERVICE)
                        .requestUri(request.getRequestURI())
                        .requestMethod(request.getMethod())
                        .ipAddress(IpUtil.getClientIp(request))
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles()
                                .stream()
                                .findFirst()
                                .map(Role::getRoleName)
                                .orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("REGISTERED_SUCCESS")
                        .entityId(user.getId())
                        .entityType("USER")
                        .description("User registered successfully")
                        .status(EventStatus.SUCCESS)
                        .createdAt(createdAt)
                        .build());
    }

    private void publishAudit(
            User user,
            String module,
            String action,
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
                        .source(EventSource.AUTH_SERVICE)
                        .requestUri(request.getRequestURI())
                        .requestMethod(request.getMethod())
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles()
                                .stream()
                                .findFirst()
                                .map(Role::getRoleName)
                                .orElse("ROLE_CUSTOMER"))
                        .module(module)
                        .action(action)
                        .entityId(user.getId())
                        .entityType("USER")
                        .description(description)
                        .status(EventStatus.SUCCESS)
                        .ipAddress(IpUtil.getClientIp(request))
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
