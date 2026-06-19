package com.bank.as.model.dtos;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;

    private String password;
}
