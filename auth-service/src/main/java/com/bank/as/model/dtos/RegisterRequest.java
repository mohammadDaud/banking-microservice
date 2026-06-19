package com.bank.as.model.dtos;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;

    private String email;

    private String password;
}
