package com.bank.common.filter;

import com.bank.common.constants.CorrelationConstants;
import com.bank.common.util.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId =
                request.getHeader(CorrelationConstants.HEADER_NAME);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        CorrelationIdUtil.setCorrelationId(correlationId);

        response.setHeader(
                CorrelationConstants.HEADER_NAME,
                correlationId
        );

        try {

            filterChain.doFilter(request, response);

        } finally {

            CorrelationIdUtil.clear();

        }
    }
}