package com.bank.exception;

public class NomineeNotFoundException extends RuntimeException {
    public NomineeNotFoundException(String message) {
        super(message);
    }
}
