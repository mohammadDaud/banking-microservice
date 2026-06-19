package com.bank.as.service;

import com.bank.as.kafka.KafkaEventPublisher;
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
import com.bank.common.events.AuditEvent;
import com.bank.common.events.EmailNotificationEvent;
import com.bank.common.events.UserRegisteredEvent;
import com.bank.common.topics.KafkaTopics;
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
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final AuditService auditService;


    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new RuntimeException(
                    "Username already exists");
        }

        if (userRepository.existsByEmail(
                request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists");
        }

        Role role =
                roleRepository
                        .findByRoleName("ROLE_CUSTOMER")
                        .orElseThrow();

        User user =
                User.builder()
                        .id(UUID.randomUUID().toString())
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .enabled(true)
                        .roles(Set.of(role))
                        .build();
        User savedUser =
                userRepository.save(user);
        String tokenemail = UUID.randomUUID().toString();
        emailVerificationTokenRepository.save(
                EmailVerificationToken.builder()
                        .token(tokenemail)
                        .userId(user.getId())
                        .expiryTime(LocalDateTime.now().plusHours(24))
                        .verified(false)
                        .build());
        kafkaEventPublisher.publish(
                KafkaTopics.USER_REGISTRATION_TOPIC,
                UserRegisteredEvent
                        .builder()
                        .userId(savedUser.getId())
                        .username(savedUser.getUsername())
                        .email(savedUser.getEmail())
                        .build()
        );
        /*userRegistrationProducer
                .publishUserRegisteredEvent(UserRegisteredEvent
                                .builder()
                                .userId(savedUser.getId())
                                .username(savedUser.getUsername())
                                .email(savedUser.getEmail())
                                .build()
                );*/
         /*emailService.sendVerificationEmail(
                user.getEmail(),
                tokenemail);*/
        /*notificationEventProducer
         .sendEmailEvent(
                 EmailNotificationEvent
                         .builder()
                         .to(user.getEmail())
                         .subject("Verify Your Email")
                         .body("Click the link:\n"
                                 + "http://localhost:8081/api/auth/verify-email?token="
                                 + tokenemail)
                         .build());*/
        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent
                        .builder()
                        .to(user.getEmail())
                        .subject("Verify Your Email")
                        .body("Click the link:\n"
                                + "http://localhost:8081/api/auth/verify-email?token="
                                + tokenemail)
                        .build()
        );
        passwordHistoryRepository.save(
                PasswordHistory.builder()
                        .userId(savedUser.getId())
                        .passwordHash(
                                savedUser.getPassword())
                        .createdAt(
                                LocalDateTime.now())
                        .build());
        /*auditService.audit(
                savedUser.getId(),
                savedUser.getUsername(),
                "USER_REGISTERED",
                IpUtil.getClientIp(httpRequest),
                true,
                "User registered successfully");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(savedUser.getId())
                        .username(savedUser.getUsername())
                        .role(savedUser.getRoles()
                                .stream()
                                .findFirst()
                                .map(Role::getRoleName)
                                .orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("REGISTERED_SUCCESS")
                        .entityId(savedUser.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(httpRequest))
                        .description("User registered successfully")
                        .createdAt(LocalDateTime.now())
                        .build()

        );
    }

    @Transactional
    public void verifyEmail(String token) {

        EmailVerificationToken verificationToken =
                emailVerificationTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid token"));

        if (verificationToken
                .getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Verification token expired");
        }

        User user =
                userRepository.findById(
                                verificationToken.getUserId())
                        .orElseThrow();

        user.setEmailVerified(true);

        userRepository.save(user);

        verificationToken.setVerified(true);
    }
}
