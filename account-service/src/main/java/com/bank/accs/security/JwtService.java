package com.bank.accs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Extract username (subject) from JWT.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract userId claim.
     */
    public String extractUserId(String token) {
        return extractAllClaims(token)
                .get("userId", String.class);
    }

    /**
     * Extract roles claim.
     */
    public List<String> extractRoles(String token) {

        Object roles =
                extractAllClaims(token)
                        .get("roles");

        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }

    /**
     * Extract token type.
     *
     * Expected values:
     * USER
     * SERVICE
     * REFRESH
     */
    public String extractTokenType(String token) {

        return extractAllClaims(token)
                .get("tokenType", String.class);
    }

    /**
     * Check whether token has expired.
     */
    public boolean isTokenExpired(String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    /**
     * Validate JWT signature and expiry.
     */
    public boolean isTokenValid(String token) {

        try {

            extractAllClaims(token);

            return !isTokenExpired(token);

        } catch (Exception ex) {

            ex.printStackTrace();   // <---- ADD THIS

            return false;
        }
    }

    /**
     * Validate that the JWT belongs to an internal service.
     */
    public boolean isInternalServiceToken(String token) {

        return "SERVICE".equals(
                extractTokenType(token)
        );
    }

    /**
     * Parse all JWT claims.
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}