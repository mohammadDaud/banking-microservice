package com.bank.as.exception;

public class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException(
            String message) {

        super(message);
    }
}
