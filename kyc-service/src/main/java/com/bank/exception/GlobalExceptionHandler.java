package com.bank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(KycAlreadyExistsException.class)
    public ResponseEntity<?> handleExists(KycAlreadyExistsException ex) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                                "timestamp",
                                LocalDateTime.now(),
                                "message",
                                ex.getMessage()
                        )
                );
    }

    @ExceptionHandler(KycNotFoundException.class)
    public ResponseEntity<?> handleNotFound(KycNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                                "timestamp",
                                LocalDateTime.now(),
                                "message",
                                ex.getMessage()
                        )
                );
    }
}