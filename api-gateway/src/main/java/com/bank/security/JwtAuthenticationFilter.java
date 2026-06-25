package com.bank.security;

import com.bank.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final RouteRoleValidator routeRoleValidator;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-otp",
            "/api/auth/logout",
            "/api/auth/verify-email",

            "/swagger-ui",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-initializer.js",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/v3/api-docs/swagger-config",

            "/auth/v3/api-docs",
            "/user/v3/api-docs",
            "/account/v3/api-docs",
            "/transaction/v3/api-docs",
            "/beneficiary/v3/api-docs",
            "/kyc/v3/api-docs",
            "/nominee/v3/api-docs",
            "/notification/v3/api-docs",
            "/audit/v3/api-docs",
            "/rule/v3/api-docs",

            "/actuator",
            "/actuator/",
            "/webjars/"
    );

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        boolean isPublicEndpoint = OPEN_ENDPOINTS.stream()
                .anyMatch(path::startsWith);

        if (isPublicEndpoint) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing Authorization Header. Path={}", path);
            return unauthorized(exchange);
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            log.warn("Invalid JWT Token. Path={}", path);
            return unauthorized(exchange);
        }

        String username = jwtService.extractUsername(token);
        String userId = jwtService.extractUserId(token);
        List<String> roles = jwtService.extractRoles(token);

        if (!routeRoleValidator.hasAccess(
                path,
                exchange.getRequest().getMethod(),
                roles
        )) {
            log.warn(
                    "Access denied. User={} Roles={} Path={}",
                    username,
                    roles,
                    path
            );

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        /*
         * Security:
         * Never trust identity headers sent by browser/Postman.
         * Remove them first, then insert values extracted from the valid JWT.
         */
        ServerHttpRequest trustedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-User");
                    headers.remove("X-User-Id");
                    headers.remove("X-Roles");

                    headers.add("X-User", username);
                    headers.add("X-User-Id", userId);
                    headers.add("X-Roles", String.join(",", roles));
                })
                .build();

        ServerWebExchange trustedExchange = exchange.mutate()
                .request(trustedRequest)
                .build();

        log.info(
                "Authenticated user={} roles={} path={}",
                username,
                roles,
                path
        );

        return chain.filter(trustedExchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}