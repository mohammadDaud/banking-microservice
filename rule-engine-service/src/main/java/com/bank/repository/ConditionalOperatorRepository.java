package com.bank.repository;

import com.bank.model.ConditionalOperator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConditionalOperatorRepository extends JpaRepository<ConditionalOperator, Long> {

    boolean existsByShortName(String shortName);

    Optional<ConditionalOperator> findByShortName(String shortName);

    List<ConditionalOperator> findByIsActiveTrueOrderByDisplayNameAsc();
}