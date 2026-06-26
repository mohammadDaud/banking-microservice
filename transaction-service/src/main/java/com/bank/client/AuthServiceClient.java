package com.bank.client;

import com.bank.dtos.InternalTokenRequest;
import com.bank.dtos.InternalTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "auth-service",
        path = "/api/auth/internal"
)
public interface AuthServiceClient {

    @PostMapping("/token")
    InternalTokenResponse generateInternalToken(@RequestBody InternalTokenRequest request);
}