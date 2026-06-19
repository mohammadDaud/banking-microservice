package com.bank.as.service;

import com.bank.as.exception.InvalidRefreshTokenException;
import com.bank.as.model.entites.RefreshToken;
import com.bank.as.model.entites.User;
import com.bank.as.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public RefreshToken createRefreshToken(
            User user) {

        List<RefreshToken> existingTokens =
                refreshTokenRepository
                        .findByUserId(
                                user.getId());

        for (RefreshToken token : existingTokens) {

            token.setRevoked(true);
            token.setActive(false);
            token.setRevokedAt(
                    LocalDateTime.now());
        }

        if (!existingTokens.isEmpty()) {
            refreshTokenRepository.saveAll(
                    existingTokens);
        }

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .id(
                                UUID.randomUUID()
                                        .toString())
                        .user(user)
                        .token(
                                UUID.randomUUID()
                                        .toString())
                        .expiryDate(
                                LocalDateTime.now()
                                        .plusSeconds(
                                                refreshExpiration / 1000))
                        .revoked(false)
                        .active(true)
                        .build();

        return refreshTokenRepository.save(
                refreshToken);
    }

    public RefreshToken verifyToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                 new InvalidRefreshTokenException(
                "Invalid refresh token"));

        if (Boolean.TRUE.equals(
                refreshToken.getRevoked())) {
            throw new InvalidRefreshTokenException(
                    "Refresh token revoked");
        }
        if (refreshToken.getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidRefreshTokenException(
                    "Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(
            String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException(
                                        "Invalid refresh token"));

        refreshToken.setRevoked(true);

        refreshToken.setActive(false);

        refreshToken.setRevokedAt(
                LocalDateTime.now());

        refreshTokenRepository.save(
                refreshToken);
    }

    public void deleteToken(
            RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    public Optional<RefreshToken>  findByToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken);
    }

    public void logout(String refreshToken) {
        RefreshToken token = findByToken(refreshToken)
                .orElseThrow(() ->
                new InvalidRefreshTokenException(
                        "Invalid refresh token"));
        revokeToken(token.getToken());
    }
}
