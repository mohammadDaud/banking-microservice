package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTokenRequest {

    /**
     * Technical service identifier.
     * Example:
     * transaction-service
     */
    private String clientId;

    /**
     * Technical service secret.
     * Loaded from environment variables.
     */
    private String clientSecret;

}