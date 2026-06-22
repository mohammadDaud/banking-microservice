package com.bank.repository;

import com.bank.model.FieldMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FieldMasterRepository extends JpaRepository<FieldMaster, Long> {

    boolean existsByFieldName(String fieldName);

    Optional<FieldMaster> findByFieldName(String fieldName);

    List<FieldMaster> findByIsActiveTrueOrderByDisplayNameAsc();
}