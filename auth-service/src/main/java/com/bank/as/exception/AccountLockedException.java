package com.bank.as.exception;

public class AccountLockedException
        extends RuntimeException {

    public AccountLockedException(
            String message) {

        super(message);
    }
}