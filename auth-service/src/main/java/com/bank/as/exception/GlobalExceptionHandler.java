package com.bank.as.exception;

import com.bank.as.model.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            RuntimeException.class)
    public ResponseEntity<ErrorResponse>
    handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.BAD_REQUEST.value())
                        .error(
                                HttpStatus.BAD_REQUEST
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error(
                                HttpStatus.INTERNAL_SERVER_ERROR
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(
            UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse>
    handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.CONFLICT.value())
                        .error(
                                HttpStatus.CONFLICT
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(
            InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.UNAUTHORIZED.value())
                        .error(
                                HttpStatus.UNAUTHORIZED
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    @ExceptionHandler(
            AccountLockedException.class)
    public ResponseEntity<ErrorResponse>
    handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.LOCKED.value())
                        .error(
                                "ACCOUNT_LOCKED")
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.LOCKED)
                .body(response);
    }

    @ExceptionHandler(
            InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.UNAUTHORIZED.value())
                        .error(
                                HttpStatus.UNAUTHORIZED
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(
            EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse>
    handleEmailNotVerified(
            EmailNotVerifiedException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.FORBIDDEN.value())
                        .error(
                                HttpStatus.FORBIDDEN
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(
            OtpNotVerifiedException.class)
    public ResponseEntity<ErrorResponse>
    handleOtpNotVerified(
            OtpNotVerifiedException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.FORBIDDEN.value())
                        .error(
                                HttpStatus.FORBIDDEN
                                        .getReasonPhrase())
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(
            InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidRestPassword(
            InvalidResetTokenException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now())
                        .status(
                                HttpStatus.BAD_REQUEST.value())
                        .error(
                                "INVALID_RESET_TOKEN")
                        .message(
                                ex.getMessage())
                        .path(
                                request.getRequestURI())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(
            PasswordReuseException.class)
    public ResponseEntity<ErrorResponse>
    handlePasswordReuseException(
            PasswordReuseException ex,
            HttpServletRequest request) {

        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("PASSWORD_REUSE")
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(response);
    }
}
