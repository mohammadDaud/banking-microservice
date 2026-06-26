package com.bank.config;

import com.bank.security.InternalServiceTokenProvider;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@RequiredArgsConstructor
public class InternalFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final InternalServiceTokenProvider tokenProvider;

    @Override
    public Exception decode(String methodKey, Response response) {

        if (response.status() == 401) {
            tokenProvider.evictToken();
            return new RetryableException(
                    response.status(),
                    "Service token expired. Retrying with new token.",
                    response.request().httpMethod(),
                    null,
                    new Date(),
                    response.request()
            );
        }
        return defaultDecoder.decode(methodKey, response);
    }
}