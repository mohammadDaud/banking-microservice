package com.bank.as.service;

import com.bank.as.model.dtos.InternalTokenRequest;
import com.bank.as.model.dtos.InternalTokenResponse;
import com.bank.as.model.entites.Role;
import com.bank.as.model.entites.User;
import com.bank.as.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalTokenService {

    private static final String TRANSACTION_SERVICE_CLIENT_ID =
            "transaction-service";

    private static final String INTERNAL_SERVICE_ROLE =
            "ROLE_INTERNAL_SERVICE";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${internal.service.transaction-service.client-secret}")
    private String transactionServiceClientSecret;

    @Value("${internal.service.token-expiration-ms}")
    private long internalTokenExpirationMs;

    public InternalTokenResponse createToken(
            InternalTokenRequest request) {

        if (request == null
                || request.getClientId() == null
                || request.getClientId().isBlank()
                || request.getClientSecret() == null
                || request.getClientSecret().isBlank()) {

            throw new RuntimeException(
                    "clientId and clientSecret are required"
            );
        }

        if (!TRANSACTION_SERVICE_CLIENT_ID.equals(
                request.getClientId())) {

            throw new RuntimeException(
                    "Invalid internal service client"
            );
        }

        /*
         * Client secret is supplied from environment variable.
         * It must never be stored in plain text in source code.
         */
        if (!passwordEncoder.matches(
                request.getClientSecret(),
                transactionServiceClientSecret)) {

            throw new RuntimeException(
                    "Invalid internal service credentials"
            );
        }

        User serviceUser = userRepository
                .findByUsername(TRANSACTION_SERVICE_CLIENT_ID)
                .orElseThrow(() -> new RuntimeException(
                        "Internal service account is not configured"
                ));

        if (!Boolean.TRUE.equals(serviceUser.getEnabled())) {
            throw new RuntimeException(
                    "Internal service account is disabled"
            );
        }

        boolean hasInternalRole = serviceUser.getRoles()
                .stream()
                .map(Role::getRoleName)
                .anyMatch(INTERNAL_SERVICE_ROLE::equals);

        if (!hasInternalRole) {
            throw new RuntimeException(
                    "Internal service account does not have "
                            + INTERNAL_SERVICE_ROLE
            );
        }

        List<String> roles = serviceUser.getRoles()
                .stream()
                .map(Role::getRoleName)
                .toList();

        String accessToken = jwtService.generateInternalServiceToken(
                serviceUser,
                roles,
                internalTokenExpirationMs
        );

        return InternalTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(internalTokenExpirationMs / 1000)
                .build();
    }
}