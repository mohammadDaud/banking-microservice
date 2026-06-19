package com.bank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankingNomineeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nominee Service API")
                        .description("Banking Microservice - banking Nominee service management")
                        .version("v1.0")
                        .contact(new Contact().name("Mohammad Daud")
                                .email("daud.mohammad1991@gmail.com"))
                        .license(new License()
                                .name("Internal Banking Application"))
                );
    }
}