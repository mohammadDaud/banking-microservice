package com.bank.exception;

public class BeneficiaryAlreadyExistsException extends RuntimeException {
    public BeneficiaryAlreadyExistsException(String message) {
        super(message);
    }
}