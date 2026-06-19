package com.bank.as.model.dtos;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}