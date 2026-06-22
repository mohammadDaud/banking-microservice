package com.bank.dtos;

import com.bank.enums.RuleDecision;
import com.bank.enums.RuleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RuleResponse {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private RuleType ruleType;
    private String description;
    private RuleDecision decision;
    private Integer priority;
    private String expression;
    private Boolean isActive;

    private List<RuleConditionResponse> conditions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}