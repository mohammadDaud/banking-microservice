package com.bank.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountFeignConfiguration {
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 2);
    }

}