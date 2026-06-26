package com.bank.config;

import com.bank.security.InternalServiceTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InternalServiceFeignInterceptor implements RequestInterceptor {

    private final InternalServiceTokenProvider tokenProvider;

    @Override
    public void apply(RequestTemplate template) {

        /*
         * Avoid sending service token when requesting
         * a new token from Auth Service itself.
         */
        if (template.path() != null &&
                template.path().startsWith("/api/auth/internal")) {
            return;
        }

        template.header(
                "Authorization",
                "Bearer " + tokenProvider.getAccessToken()
        );
    }
}