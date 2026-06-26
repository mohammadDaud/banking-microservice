package com.bank.config;


import com.bank.security.InternalServiceTokenProvider;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AccountFeignConfiguration {

    private final InternalServiceTokenProvider provider;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new InternalServiceFeignInterceptor(provider);
    }


    @Bean
    public ErrorDecoder errorDecoder() {
        return new InternalFeignErrorDecoder(provider);
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100,1000,2);
    }

}