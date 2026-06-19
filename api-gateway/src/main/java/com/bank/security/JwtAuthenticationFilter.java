package com.bank.security;

import com.bank.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
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

    private static final List<String> OPEN_ENDPOINTS =
            List.of(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh-token",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/verify-otp",
                    "/api/auth//logout",
                    "/api/auth/verify-email",
                    // Swagger UI
                    "/swagger-ui",
                    "/swagger-ui/",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/swagger-ui/swagger-initializer.js",
                    "/v3/api-docs",
                    "/v3/api-docs/",
                    "/v3/api-docs/swagger-config",

                    // Service docs
                    "/auth/v3/api-docs",
                    "/user/v3/api-docs",
                    "/account/v3/api-docs",
                    "/transaction/v3/api-docs",
                    "/beneficiary/v3/api-docs",
                    "/kyc/v3/api-docs",
                    "/nominee/v3/api-docs",
                    "/notification/v3/api-docs",
                    "/audit/v3/api-docs",
                    "/actuator",
                    "/webjars/**",
                    "/actuator/**"
            );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        /*
         * Skip authentication for public endpoints
         */
        boolean isPublicEndpoint =
                OPEN_ENDPOINTS.stream()
                        .anyMatch(path::startsWith);



        if (isPublicEndpoint) {
            return chain.filter(exchange);
        }

        /*
         * Read Authorization Header
         */
        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing Authorization Header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        /*
         * Validate JWT
         */
        if (!jwtService.validateToken(token)) {
            log.warn("Invalid JWT Token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        /*
         * Extract User Information
         */
        String username =
                jwtService.extractUsername(
                        token);

        String userId =
                jwtService.extractUserId(
                        token);

        List<String> roles =
                jwtService.extractRoles(
                        token);
        /*
         * Role Authorization
         */
        if (!routeRoleValidator.hasAccess(path,roles)) {
            log.warn("Access Denied. User={} Roles={} Path={}",
                    username,
                    roles,
                    path);

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.FORBIDDEN);

            return exchange.getResponse()
                    .setComplete();
        }

        /*
         * Pass User Information To Downstream Services
         */
        exchange =
                exchange.mutate()
                        .request(exchange.getRequest()
                                        .mutate()
                                        .header("X-User",
                                                username)
                                        .header("X-User-Id",
                                                userId)
                                        .header("X-Roles",
                                                String.join(",",roles))
                                        .build())
                        .build();

        log.info("Authenticated User={} Roles={} Path={}",
                username,
                roles,
                path);

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}