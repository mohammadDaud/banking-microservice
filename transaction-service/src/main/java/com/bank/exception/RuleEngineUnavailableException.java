package com.bank.exception;

public class RuleEngineUnavailableException extends RuntimeException {

    public RuleEngineUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}