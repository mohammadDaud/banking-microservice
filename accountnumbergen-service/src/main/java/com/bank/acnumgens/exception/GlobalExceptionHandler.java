package com.bank.acnumgens.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            ResourceNotFoundException.class)
    public ResponseEntity<?> handle(
            ResourceNotFoundException ex) {

        return ResponseEntity
                .status(
                        HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "timestamp",
                                LocalDateTime.now(),
                                "message",
                                ex.getMessage()
                        ));
    }
}