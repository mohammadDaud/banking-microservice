package com.bank.repository;

import com.bank.model.RuleCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleConditionRepository extends JpaRepository<RuleCondition, Long> {

        List<RuleCondition> findByRule_IdOrderBySequenceOrderAsc(Long ruleId);
    }