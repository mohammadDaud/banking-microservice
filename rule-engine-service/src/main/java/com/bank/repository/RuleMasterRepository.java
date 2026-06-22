package com.bank.repository;

import com.bank.enums.RuleType;
import com.bank.model.RuleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RuleMasterRepository extends JpaRepository<RuleMaster, Long> {

    boolean existsByRuleCode(String ruleCode);

    Optional<RuleMaster> findByRuleCode(String ruleCode);

    @Query("""
            SELECT DISTINCT rule
            FROM RuleMaster rule
            LEFT JOIN FETCH rule.conditions condition
            LEFT JOIN FETCH condition.field field
            LEFT JOIN FETCH condition.operator operator
            WHERE rule.ruleType = :ruleType
              AND rule.isActive = true
            ORDER BY rule.priority ASC
            """)
    List<RuleMaster> findByRuleTypeAndIsActiveTrueOrderByPriorityAsc(
            RuleType ruleType
    );
}