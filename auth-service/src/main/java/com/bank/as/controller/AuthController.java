package com.bank.as.controller;

import com.bank.as.model.dtos.*;
import com.bank.as.service.AuthService;
import com.bank.as.service.InternalTokenService;
import com.bank.as.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Authentication APIs",
        description = "Authentication, registration, OTP, refresh token APIs"
)
public class AuthController {

    private final LoginService loginService;
    private final AuthService authService;
    private final InternalTokenService internalTokenService;

    @Operation(
            summary = "Register User",
            description = "Register a new banking customer"
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(
            @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {

        authService.register(request, servletRequest);
    }

    @Operation(
            summary = "Login User",
            description = "Validate username/password and send OTP"
    )
    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        return loginService.authenticate(request, servletRequest);
    }

    @Operation(
            summary = "Verify OTP",
            description = "Verify OTP and generate JWT token"
    )
    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(
            @RequestBody VerifyOtpRequest request,HttpServletRequest servletRequest) {

        return loginService.verifyOtp(request, servletRequest);
    }

    @Operation(
            summary = "Generate internal service token",
            description = "Only for trusted microservices. "
                    + "Generates a short-lived service JWT."
    )
    @PostMapping("/internal/token")
    public InternalTokenResponse createInternalToken(
            @RequestBody InternalTokenRequest request) {

        return internalTokenService.createToken(request);
    }

    @Operation(
            summary = "Logout User",
            description = "Invalidate refresh token"
    )
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody LogoutRequest request,
            HttpServletRequest servletRequest) {

        return ResponseEntity.ok(
                loginService.logout(request, servletRequest)
        );
    }

    @Operation(
            summary = "Refresh Access Token",
            description = "Generate a new access token using refresh token"
    )
    @PostMapping("/refresh")
    public AuthResponse refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest servletRequest) {

        return loginService.refreshToken(
                request.getRefreshToken(),
                servletRequest
        );
    }

    @Operation(summary = "Verify Email")
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token,HttpServletRequest servletRequest) {
        authService.verifyEmail(token, servletRequest);
        return "Email verified successfully";
    }

    @Operation(summary = "Forgot Password")
    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestBody ForgotPasswordRequest request,HttpServletRequest servletRequest) {

        return loginService.forgotPassword(request, servletRequest);
    }

    @Operation(summary = "Reset Password")
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestBody ResetPasswordRequest request,
            HttpServletRequest servletRequest) {

        return loginService.resetPassword(request, servletRequest);
    }
}