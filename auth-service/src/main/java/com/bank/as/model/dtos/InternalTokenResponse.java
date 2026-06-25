package com.bank.as.model.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InternalTokenResponse {

    private String accessToken;

    private String tokenType;

    private Long expiresIn;
}