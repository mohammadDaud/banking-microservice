package com.bank.as.exception;

public class OtpNotVerifiedException extends RuntimeException {

    public OtpNotVerifiedException(String message) {
        super(message);
    }
}
