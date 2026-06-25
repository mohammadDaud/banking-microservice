package com.bank.as.service;

import com.bank.as.model.entites.Role;
import com.bank.as.model.entites.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getRoleName)
                .toList();

        return generateToken(
                user.getUsername(),
                user.getId(),
                roles,
                "USER",
                jwtExpiration
        );
    }

    public String generateInternalServiceToken(
            User serviceUser,
            List<String> roles,
            long expirationMs) {

        return generateToken(
                serviceUser.getUsername(),
                serviceUser.getId(),
                roles,
                "SERVICE",
                expirationMs
        );
    }

    public String generateRefreshToken(User user) {
        return generateToken(
                user.getUsername(),
                user.getId(),
                List.of(),
                "REFRESH",
                refreshExpiration
        );
    }

    private String generateToken(
            String username,
            String userId,
            List<String> roles,
            String tokenType,
            long expirationMs) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");

        if (roles instanceof List<?> roleList) {
            return roleList.stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get("tokenType", String.class);
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        try {
            String username = extractUsername(token);

            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
