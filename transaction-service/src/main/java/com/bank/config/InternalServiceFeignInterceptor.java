package com.bank.config;

import com.bank.security.InternalServiceTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class InternalServiceFeignInterceptor implements RequestInterceptor {

    private final InternalServiceTokenProvider tokenProvider;

    @Override
    public void apply(RequestTemplate template) {

        template.header(
                "Authorization",
                "Bearer " + tokenProvider.getAccessToken()
        );
    }
}