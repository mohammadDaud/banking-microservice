package com.bank.as.repository;

import com.bank.as.model.entites.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, String> {
    Optional<OtpVerification> findTopByUserIdOrderByCreatedAtDesc( String userId);
}
