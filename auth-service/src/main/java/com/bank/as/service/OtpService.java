package com.bank.as.service;

import com.bank.as.model.entites.OtpVerification;
import com.bank.as.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository repository;

    public String createOtp(
            String userId) {

        String otp =
                String.valueOf(
                        ThreadLocalRandom.current()
                                .nextInt(
                                        100000,
                                        999999));

        OtpVerification entity =
                OtpVerification.builder()
                        .userId(userId)
                        .otp(otp)
                        .verified(false)
                        .createdAt(
                                LocalDateTime.now())
                        .expiryTime(
                                LocalDateTime.now()
                                        .plusMinutes(5))
                        .build();

        repository.save(entity);

        return otp;
    }
}