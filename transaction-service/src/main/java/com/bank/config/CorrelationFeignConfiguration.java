package com.bank.config;

import com.bank.common.constants.CorrelationConstants;
import com.bank.common.util.CorrelationIdUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorrelationFeignConfiguration {

    @Bean
    public RequestInterceptor correlationRequestInterceptor() {

        return requestTemplate -> {

            String correlationId =
                    CorrelationIdUtil.getCorrelationId();

            if (correlationId != null &&
                    !correlationId.isBlank()) {

                requestTemplate.header(
                        CorrelationConstants.HEADER_NAME,
                        correlationId);
            }

        };
    }
}