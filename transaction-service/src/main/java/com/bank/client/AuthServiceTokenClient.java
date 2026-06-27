package com.bank.client;

import com.bank.dtos.InternalTokenRequest;
import com.bank.dtos.InternalTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceTokenClient {

    private static final String AUTH_SERVICE_URL =
            "http://auth-service/api/auth/internal/token";

    private final RestClient.Builder restClientBuilder;

    public InternalTokenResponse generateInternalToken(InternalTokenRequest request) {
        log.info("Requesting internal service token from Auth Service.");
        return restClientBuilder
                .build()
                .post()
                .uri(AUTH_SERVICE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<InternalTokenResponse>() {});
    }
}