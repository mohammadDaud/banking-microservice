package com.bank.us.exception;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(String userId) {
        super("Customer not found : " + userId);
    }

}