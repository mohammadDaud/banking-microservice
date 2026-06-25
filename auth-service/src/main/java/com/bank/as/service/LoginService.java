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

    private static final String ROLE_INTERNAL_SERVICE =
            "ROLE_INTERNAL_SERVICE";

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
    private final KafkaEventPublisher kafkaEventPublisher;

    @Value("${security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${security.lock-duration-minutes}")
    private long lockDuration;

    public LoginResponse authenticate(
            LoginRequest request,
            HttpServletRequest servletRequest) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> {

                    loginAuditService.saveAudit(
                            null,
                            request.getUsername(),
                            IpUtil.getClientIp(servletRequest),
                            false,
                            "USER_NOT_FOUND"
                    );

                    return new InvalidCredentialsException(
                            "Invalid username or password"
                    );
                });

        /*
         * Technical accounts must never use the customer/admin OTP login flow.
         * They receive a short-lived JWT only from:
         * POST /api/auth/internal/token
         */
        if (isInternalServiceAccount(user)) {
            throw new InvalidCredentialsException(
                    "Internal service accounts cannot use user login"
            );
        }

        checkLockStatus(user, servletRequest);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (Exception ex) {
            increaseFailedAttempts(user, servletRequest);

            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException(
                    "Please verify your email first."
            );
        }

        resetFailedAttempts(user);

        String otp = otpService.createOtp(user.getId());

        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Your OTP Code")
                        .body(
                                "Your OTP code is: "
                                        + otp
                                        + "\n\nThis code expires in 5 minutes."
                        )
                        .build()
        );

        publishAudit(
                user,
                "AUTH",
                "OTP_SUCCESS",
                "OTP sent successfully",
                servletRequest
        );

        return LoginResponse.builder()
                .message("OTP sent successfully")
                .otpRequired(true)
                .build();
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "User not found"
                ));

        if (isInternalServiceAccount(user)) {
            throw new InvalidCredentialsException(
                    "Internal service accounts cannot verify OTP"
            );
        }

        OtpVerification otpEntity = otpRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new OtpNotVerifiedException(
                        "OTP not found"
                ));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new OtpNotVerifiedException("OTP expired");
        }

        if (!otpEntity.getOtp().equals(request.getOtp())) {
            throw new OtpNotVerifiedException("Invalid OTP");
        }

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);

        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = refreshTokenService
                .createRefreshToken(user);

        String role = user.getRoles()
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

    public String forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new EmailNotVerifiedException(
                        "Email not found"
                ));

        if (isInternalServiceAccount(user)) {
            throw new InvalidCredentialsException(
                    "Password reset is not allowed for internal service accounts"
            );
        }

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiryTime(LocalDateTime.now().plusMinutes(15))
                .build();

        passwordResetTokenRepository.save(resetToken);

        String resetUrl =
                "http://localhost:8081/api/auth/reset-password?token="
                        + token;

        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Password Reset Request")
                        .body(
                                "Click below link to reset your password:\n\n"
                                        + resetUrl
                                        + "\n\nThis link expires in 15 minutes."
                        )
                        .build()
        );

        return "Password reset link sent successfully";
    }

    @Transactional
    public String resetPassword(
            ResetPasswordRequest request,
            HttpServletRequest servletRequest) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new InvalidResetTokenException(
                        "Invalid reset token"
                ));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new InvalidResetTokenException(
                    "Reset token already used"
            );
        }

        if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("Reset token expired");
        }

        User user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException(
                        "User not found"
                ));

        if (isInternalServiceAccount(user)) {
            throw new InvalidCredentialsException(
                    "Password reset is not allowed for internal service accounts"
            );
        }

        validatePasswordHistory(user, request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordHistoryRepository.save(
                PasswordHistory.builder()
                        .userId(user.getId())
                        .passwordHash(user.getPassword())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        kafkaEventPublisher.publish(
                KafkaTopics.EMAIL_NOTIFICATION_TOPIC,
                EmailNotificationEvent.builder()
                        .to(user.getEmail())
                        .subject("Password Changed Successfully")
                        .body("Your password has been updated successfully.")
                        .build()
        );

        publishAudit(
                user,
                "USER",
                "PASSWORD_RESET",
                "Password changed successfully",
                servletRequest
        );

        return "Password reset successful";
    }

    public AuthResponse refreshToken(
            String refreshTokenValue,
            HttpServletRequest servletRequest) {

        RefreshToken oldToken = refreshTokenService
                .verifyToken(refreshTokenValue);

        User user = oldToken.getUser();

        if (isInternalServiceAccount(user)) {
            throw new InvalidRefreshTokenException(
                    "Internal service accounts cannot use refresh tokens"
            );
        }

        refreshTokenService.revokeToken(oldToken.getToken());

        RefreshToken newToken = refreshTokenService
                .createRefreshToken(user);

        String accessToken = jwtService.generateAccessToken(user);

        publishAudit(
                user,
                "AUTH",
                "REFRESH_TOKEN_USED",
                "Access token refreshed successfully",
                servletRequest
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public String logout(
            LogoutRequest request,
            HttpServletRequest httpRequest) {

        RefreshToken refreshToken = refreshTokenService
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Invalid refresh token"
                ));

        User user = refreshToken.getUser();

        if (isInternalServiceAccount(user)) {
            throw new InvalidRefreshTokenException(
                    "Internal service accounts cannot logout with refresh tokens"
            );
        }

        refreshTokenService.logout(request.getRefreshToken());

        publishAudit(
                user,
                "AUTH",
                "LOGOUT",
                "User logged out successfully",
                httpRequest
        );

        return "Logout successful";
    }

    private void validatePasswordHistory(
            User user,
            String newPassword) {

        List<PasswordHistory> histories = passwordHistoryRepository
                .findTop3ByUserIdOrderByCreatedAtDesc(user.getId());

        boolean reused = histories.stream()
                .anyMatch(history -> passwordEncoder.matches(
                        newPassword,
                        history.getPasswordHash()
                ));

        if (reused) {
            throw new PasswordReuseException(
                    "You cannot reuse any of your last 3 passwords."
            );
        }
    }

    private void increaseFailedAttempts(
            User user,
            HttpServletRequest request) {

        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);

        publishAudit(
                user,
                "AUTH",
                "INVALID_PASSWORD",
                "Password invalid",
                request
        );

        if (attempts >= maxFailedAttempts) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());

            publishAudit(
                    user,
                    "AUTH",
                    "ACCOUNT_BLOCKED",
                    "Account blocked after too many failed login attempts",
                    request
            );
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    private void checkLockStatus(
            User user,
            HttpServletRequest request) {

        if (!Boolean.TRUE.equals(user.getAccountLocked())) {
            return;
        }

        LocalDateTime lockTime = user.getLockTime();

        if (lockTime == null) {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            userRepository.save(user);
            return;
        }

        LocalDateTime unlockTime = lockTime.plusMinutes(lockDuration);

        if (LocalDateTime.now().isAfter(unlockTime)) {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
            return;
        }

        publishAudit(
                user,
                "AUTH",
                "ACCOUNT_BLOCKED",
                "Account is currently locked",
                request
        );

        throw new AccountLockedException(
                "Account locked. Try again later."
        );
    }

    private boolean isInternalServiceAccount(User user) {
        return user.getRoles() != null
                && user.getRoles()
                .stream()
                .anyMatch(role -> ROLE_INTERNAL_SERVICE.equals(
                        role.getRoleName()
                ));
    }

    private void publishAudit(
            User user,
            String module,
            String action,
            String description,
            HttpServletRequest request) {

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
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
                        .ipAddress(IpUtil.getClientIp(request))
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}