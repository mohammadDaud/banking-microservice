package com.bank.accs.exception;

public class FeignServiceException extends RuntimeException {
    public FeignServiceException( String message) {
        super(message);
    }
    public FeignServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}