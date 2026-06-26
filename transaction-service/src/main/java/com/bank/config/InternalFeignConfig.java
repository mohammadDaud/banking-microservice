package com.bank.config;

import com.bank.security.InternalServiceTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InternalFeignConfig {

    private final InternalServiceTokenProvider tokenProvider;

    @Bean
    public RequestInterceptor internalServiceRequestInterceptor() {
        return template -> template.header(
                "Authorization",
                "Bearer " + tokenProvider.getAccessToken()
        );
    }
}