package com.bank.config;

import com.bank.security.InternalServiceTokenProvider;
import feign.RequestInterceptor;
import feign.Retryer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AccountFeignConfiguration {

    private final InternalServiceTokenProvider tokenProvider;

    @Bean
    public RequestInterceptor internalServiceRequestInterceptor() {

        return requestTemplate -> {

            String token = tokenProvider.getAccessToken();

            log.info("Adding Internal JWT to Feign Request");

            requestTemplate.header(
                    "Authorization",
                    "Bearer " + token
            );
        };
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 2);
    }

}