package com.bank.repository;

import com.bank.model.FieldOperatorMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldOperatorMappingRepository extends JpaRepository<FieldOperatorMapping, Long> {

    List<FieldOperatorMapping> findByFieldId(Long fieldId);

    boolean existsByFieldIdAndOperatorId(Long fieldId, Long operatorId);

    void deleteByFieldIdAndOperatorId(Long fieldId, Long operatorId);
}