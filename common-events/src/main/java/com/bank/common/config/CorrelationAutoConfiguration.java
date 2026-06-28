package com.bank.common.config;

import com.bank.common.filter.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CorrelationAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationFilter() {

        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new CorrelationIdFilter());

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        registration.addUrlPatterns("/*");

        return registration;
    }
}