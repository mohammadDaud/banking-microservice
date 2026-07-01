package com.bank.us.repository;

import com.bank.us.model.KycDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository extends JpaRepository<KycDetail, String> {

    Optional<KycDetail> findByUserId(String userId);
}