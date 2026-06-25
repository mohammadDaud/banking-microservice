package com.bank.as.model.dtos;

import lombok.Data;

@Data
public class InternalTokenRequest {

    private String clientId;

    private String clientSecret;
}