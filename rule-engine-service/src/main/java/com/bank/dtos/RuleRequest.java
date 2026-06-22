package com.bank.dtos;

import com.bank.enums.RuleDecision;
import com.bank.enums.RuleType;
import lombok.Data;

import java.util.List;

@Data
public class RuleRequest {

    private String ruleCode;

    private String ruleName;

    private RuleType ruleType;

    private String description;

    private RuleDecision decision;

    private Integer priority = 100;

    /*
     * Leave null for now.
     * Step 6 will support: (1 AND 2) OR 3
     */
    private String expression;

    private Boolean isActive = true;

    // Temporary. Later get it from JWT.
    private String requestedBy;

    //@NotEmpty(message = "At least one rule condition is required")
    private List<RuleConditionRequest> conditions;
}