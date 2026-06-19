package com.bank.as.service;

import com.bank.as.exception.*;
import com.bank.as.kafka.KafkaEventPublisher;
import com.bank.as.model.dtos.*;
import com.bank.as.model.entites.*;
import com.bank.as.repository.OtpVerificationRepository;
import com.bank.as.repository.PasswordHistoryRepository;
import com.bank.as.repository.PasswordResetTokenRepository;
import com.bank.as.repository.UserRepository;
import com.bank.as.utill.IpUtil;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.EmailNotificationEvent;
import com.bank.common.topics.KafkaTopics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAuditService loginAuditService;
    private final OtpService otpService;
    private final OtpVerificationRepository otpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final AuditService auditService;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Value("${security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.lock-duration-minutes}")
    private long lockDuration;


    public LoginResponse authenticate( LoginRequest request, HttpServletRequest servletRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
        User user =
                userRepository
                        .findByUsername(
                                request.getUsername())
                        .orElseThrow(() -> {

                            loginAuditService.saveAudit(
                                    null,
                                    request.getUsername(),
                                    IpUtil.getClientIp(
                                            servletRequest),
                                    false,
                                    "USER_NOT_FOUND");

                            return new InvalidCredentialsException(
                                    "Invalid username or password");
                        });

        if (!Boolean.TRUE.equals(
                user.getEmailVerified())) {

            throw new EmailNotVerifiedException(
                    "Please verify your email first.");
        }

        checkLockStatus(
                user,
                servletRequest);

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            increaseFailedAttempts(
                    user,
                    servletRequest);

            throw new InvalidCredentialsException(
                    "Invalid username or password");
        }

        resetFailedAttempts(user);

        String otp =
                otpService.createOtp(
                        user.getId());

/*notificationEventProducer
        .sendEmailEvent(
                 EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Your OTP Code")
                        .body("Your OTP code is: " + otp + "\n\nThis code expires in 5 minutes.")
                        .build()
                        );*/

        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent
                        .builder()
                        .to(user.getEmail())
                        .subject("Your OTP Code")
                        .body("Your OTP code is: " + otp + "\n\nThis code expires in 5 minutes.")
                        .build()
        );
        /*loginAuditService.saveAudit(
                user.getId(),
                user.getUsername(),
                IpUtil.getClientIp(
                        servletRequest),
                true,
                "OTP_SENT");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("CUSTOMER"))
                        .module("AUTH")
                        .action("OTP_SUCCESS")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(servletRequest))
                        .description("Otp Send successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return LoginResponse.builder()
                .message(
                        "OTP sent successfully")
                .otpRequired(true)
                .build();
    }

    public AuthResponse verifyOtp(
            VerifyOtpRequest request) {

        User user =
                userRepository
                        .findByUsername(
                                request.getUsername())
                        .orElseThrow(() ->
                                new InvalidCredentialsException(
                                        "User not found"));

        OtpVerification otpEntity =
                otpRepository
                        .findTopByUserIdOrderByCreatedAtDesc(
                                user.getId())
                        .orElseThrow(() ->
                                new OtpNotVerifiedException(
                                        "OTP not found"));

        if (otpEntity.getExpiryTime()
                .isBefore(
                        LocalDateTime.now())) {

            throw new OtpNotVerifiedException(
                    "OTP expired");
        }

        if (!otpEntity.getOtp()
                .equals(
                        request.getOtp())) {

            throw new OtpNotVerifiedException(
                    "Invalid OTP");
        }

        otpEntity.setVerified(true);

        otpRepository.save(
                otpEntity);

        String accessToken =
                jwtService.generateToken(
                        user);

        RefreshToken refreshToken =
                refreshTokenService
                        .createRefreshToken(
                                user);
        String role =
                user.getRoles()
                        .stream()
                        .findFirst()
                        .map(Role::getRoleName)
                        .orElse("ROLE_CUSTOMER");
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(role)
                .build();
    }

    public String forgotPassword(
            ForgotPasswordRequest request) {

        User user =
                userRepository
                        .findByEmail(
                                request.getEmail())
                        .orElseThrow(() ->
                                new EmailNotVerifiedException(
                                        "Email not found"));

        String token =
                UUID.randomUUID().toString();

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .token(token)
                        .userId(user.getId())
                        .used(false)
                        .createdAt(
                                LocalDateTime.now())
                        .expiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(15))
                        .build();

        passwordResetTokenRepository
                .save(resetToken);

        String resetUrl =
                "http://localhost:8081/api/auth/reset-password?token="
                        + token;

        /*emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetUrl);*/
        /*notificationEventProducer.sendEmailEvent(EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Password Reset Request")
                        .body("Click below link to reset your password:\n\n"
                                + resetUrl +
                                "\n\nThis link expires in 15 minutes.")
                        .build());*/
        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent
                        .builder()
                        .to(user.getEmail())
                        .subject("Password Reset Request")
                        .body("Click below link to reset your password:\n\n"
                                + resetUrl +
                                "\n\nThis link expires in 15 minutes.")
                        .build()
        );
        return "Password reset link sent successfully";
    }

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request,HttpServletRequest servletRequest) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByToken(
                                request.getToken())
                        .orElseThrow(() ->
                                new InvalidResetTokenException(
                                        "Invalid reset token"));

        if (Boolean.TRUE.equals(
                resetToken.getUsed())) {

            throw new InvalidResetTokenException(
                    "Reset token already used");
        }

        if (resetToken.getExpiryTime()
                .isBefore(
                        LocalDateTime.now())) {

            throw new InvalidResetTokenException(
                    "Reset token expired");
        }

        User user =
                userRepository
                        .findById(
                                resetToken.getUserId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        validatePasswordHistory(
                user,
                request.getNewPassword());

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepository.save(user);

        // SAVE NEW PASSWORD INTO HISTORY
        passwordHistoryRepository.save(
                PasswordHistory.builder()
                        .userId(
                                user.getId())
                        .passwordHash(
                                user.getPassword())
                        .createdAt(
                                LocalDateTime.now())
                        .build());

        resetToken.setUsed(true);

        passwordResetTokenRepository.save(
                resetToken);

        /*emailService.sendEmailMsg(
                user.getEmail(),
                "Password Changed Successfully",
                "Your password has been updated successfully.");*/
       /* notificationEventProducer.sendEmailEvent(EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Password Changed Successfully")
                        .body("Your password has been updated successfully.")
                        .build());*/
        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent
                        .builder()
                        .to(user.getEmail())
                        .subject("Password Changed Successfully")
                        .body("Your password has been updated successfully.")
                        .build()
        );
        /*auditService.audit(
                user.getId(),
                user.getUsername(),
                "PASSWORD_RESET_COMPLETED",
                IpUtil.getClientIp(servletRequest),
                true,
                "Password changed successfully");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                        .module("USER")
                        .action("PASSWORD_RESET")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(servletRequest))
                        .description("Password changed successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
                );

        return "Password reset successful";
    }

    private void validatePasswordHistory(
            User user,
            String newPassword) {

        List<PasswordHistory> histories =
                passwordHistoryRepository
                        .findTop3ByUserIdOrderByCreatedAtDesc(
                                user.getId());

        boolean reused =
                histories.stream()
                        .anyMatch(history ->
                                passwordEncoder.matches(
                                        newPassword,
                                        history.getPasswordHash()));

        if (reused) {
            throw new PasswordReuseException(
                    "You cannot reuse any of your last 3 passwords.");
        }
    }



    private void increaseFailedAttempts(
            User user,
            HttpServletRequest request) {

        int attempts =
                user.getFailedAttempts() + 1;

        user.setFailedAttempts(
                attempts);

        /*loginAuditService.saveAudit(
                user.getId(),
                user.getUsername(),
                IpUtil.getClientIp(request),
                false,
                "INVALID_PASSWORD");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("INVALID_PASSWORD")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(request))
                        .description("Password Invalid!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        if (attempts >= maxFailedAttempts) {

            user.setAccountLocked(true);
            user.setLockTime(
                    LocalDateTime.now());

            /*loginAuditService.saveAudit(
                    user.getId(),
                    user.getUsername(),
                    IpUtil.getClientIp(request),
                    false,
                    "ACCOUNT_LOCKED");*/
            kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                    AuditEvent.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                            .module("AUTH")
                            .action("ACCOUNT_BLOCKED")
                            .entityId(user.getId())
                            .entityType("USER")
                            .ipAddress(IpUtil.getClientIp(request))
                            .description("Account Blocked!")
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        userRepository.save(user);
    }
    private void checkLockStatus(User user,HttpServletRequest request) {
        if (!Boolean.TRUE.equals(user.getAccountLocked())) {
            return;
        }

        LocalDateTime unlockTime =
                user.getLockTime()
                        .plusMinutes(lockDuration);

        if (LocalDateTime.now().isAfter(unlockTime)) {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
            return;
        }

       /* loginAuditService.saveAudit(
                user.getId(),
                user.getUsername(),
                IpUtil.getClientIp(request),false,"ACCOUNT_LOCKED");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("ACCOUNT_BLOCKED")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(request))
                        .description("Account Blocked!")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        throw new AccountLockedException("Account locked. Try again later.");
    }

    public AuthResponse refreshToken(String refreshTokenValue,HttpServletRequest servletRequest) {
        RefreshToken oldToken =
                refreshTokenService
                        .verifyToken(refreshTokenValue);

        User user = oldToken.getUser();
        refreshTokenService
                .revokeToken(oldToken.getToken());

        RefreshToken newToken =
                refreshTokenService
                        .createRefreshToken(user);

        String accessToken =
                jwtService.generateToken(user);

        /*auditService.audit(
                user.getId(),
                user.getUsername(),
                "REFRESH_TOKEN_USED",
                IpUtil.getClientIp(servletRequest),
                true,
                "Access token refreshed successfully");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("REFRESH_TOKEN_USED")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(servletRequest))
                        .description("Access token refreshed successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public String logout(LogoutRequest request,HttpServletRequest httpRequest) {

        RefreshToken refreshToken =
                refreshTokenService
                        .findByToken(request.getRefreshToken())
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException("Invalid refresh token"));

        User user = refreshToken.getUser();
        refreshTokenService.logout(request.getRefreshToken());
        /*auditService.audit(
                user.getId(),
                user.getUsername(),
                "LOGOUT",
                IpUtil.getClientIp(httpRequest),
                true,
                "User logged out successfully");*/
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRoles().stream().findFirst().map(Role::getRoleName).orElse("ROLE_CUSTOMER"))
                        .module("AUTH")
                        .action("LOGOUT")
                        .entityId(user.getId())
                        .entityType("USER")
                        .ipAddress(IpUtil.getClientIp(httpRequest))
                        .description("User logged out successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return "Logout successful";
    }
}
