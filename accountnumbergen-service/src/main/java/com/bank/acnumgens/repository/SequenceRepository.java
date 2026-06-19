package com.bank.acnumgens.repository;

import com.bank.acnumgens.model.SequenceMaster;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SequenceRepository extends JpaRepository<SequenceMaster, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM SequenceMaster s
            WHERE s.sequenceType = :type
            """)
    Optional<SequenceMaster>
    findBySequenceTypeForUpdate(
            @Param("type")
            String type);
}