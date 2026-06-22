package com.bank.repository;

import com.bank.enums.KycStatus;
import com.bank.model.KycProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KycRepository extends JpaRepository<KycProfile,String> {
    Optional<KycProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
    List<KycProfile> findByKycStatus(KycStatus status);

    @Query("""
            SELECT k.kycStatus, COUNT(k)
            FROM KycProfile k
            GROUP BY k.kycStatus
            """)
    List<Object[]> getStats();

    @Query("""
            SELECT COUNT(k)
            FROM KycProfile k
            WHERE CAST(k.kycStatus AS string) = :status
            """)
    Long countByKycStatus(String status);
}