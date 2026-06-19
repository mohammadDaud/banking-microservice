package com.bank.accs.config;


import com.bank.accs.exception.FeignServiceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignErrorDecoder {

    @Bean
    public ErrorDecoder errorDecoder() {

        return (methodKey, response) -> {

            return switch (
                    response.status()) {

                case 400 ->
                        new FeignServiceException("Invalid request sent to Account Number Generator Service");

                case 404 ->
                        new FeignServiceException("Account Number Generator Service endpoint not found");

                case 500 ->
                        new FeignServiceException("Account Number Generator Service internal error");

                case 503 ->
                        new FeignServiceException("Account Number Generator Service unavailable");

                default ->
                        new FeignServiceException("Feign call failed. HTTP Status: "+ response.status());
            };
        };
    }
}