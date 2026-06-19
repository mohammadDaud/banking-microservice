package com.bank.as.model.dtos;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String username;

    private String otp;
}