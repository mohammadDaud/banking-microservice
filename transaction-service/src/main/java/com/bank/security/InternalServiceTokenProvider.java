package com.bank.security;

import com.bank.client.AuthServiceTokenClient;
import com.bank.dtos.InternalTokenRequest;
import com.bank.dtos.InternalTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalServiceTokenProvider {

    private final AuthServiceTokenClient tokenClient;

    @Value("${internal.auth.client-id}")
    private String clientId;

    @Value("${internal.auth.client-secret}")
    private String clientSecret;

    /**
     * Cached service JWT.
     */
    private volatile String cachedToken;

    /**
     * Cached token expiry time.
     */
    private volatile Instant expiryTime;

    /**
     * Ensures only one thread refreshes the token.
     */
    private final ReentrantLock refreshLock = new ReentrantLock();

    /**
     * Returns a valid service token.
     */
    public String getAccessToken() {

        if (isTokenValid()) {
            return cachedToken;
        }

        refreshLock.lock();

        try {

            /*
             * Another thread may already have refreshed it.
             */
            if (isTokenValid()) {
                return cachedToken;
            }

            log.info("Fetching new internal service token from Auth Service.");

           if(new BCryptPasswordEncoder().matches("TRANSACTION_SERVICE_CLIENT_SECRET_HASH",clientSecret)) {
               clientSecret="TRANSACTION_SERVICE_CLIENT_SECRET_HASH";
           }
               InternalTokenResponse response =
                       tokenClient.generateInternalToken(
                               InternalTokenRequest.builder()
                                       .clientId(clientId)
                                       .clientSecret(clientSecret)
                                       .build()
                       );

            cachedToken = response.getAccessToken();

            /*
             * Auth Service returns expiresIn in seconds.
             * Refresh 30 seconds before expiry.
             */
            expiryTime = Instant.now()
                    .plusSeconds(response.getExpiresIn());

            log.info("Internal service token cached successfully.");

            return cachedToken;

        } finally {

            refreshLock.unlock();

        }
    }

    /**
     * Called when Account Service returns 401.
     * Forces the next request to obtain a fresh JWT.
     */
    public void evictToken() {

        refreshLock.lock();

        try {

            cachedToken = null;
            expiryTime = null;

            log.warn("Cached internal service token evicted.");

        } finally {

            refreshLock.unlock();

        }
    }

    /**
     * Returns true if cached token is still valid.
     * Refresh 30 seconds before expiry.
     */
    private boolean isTokenValid() {

        return cachedToken != null
                && expiryTime != null
                && Instant.now().isBefore(
                expiryTime.minusSeconds(30)
        );
    }

}