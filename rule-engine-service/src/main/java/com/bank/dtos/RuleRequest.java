package com.bank.dtos;

import com.bank.enums.RuleDecision;
import com.bank.enums.RuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RuleRequest {

    @NotBlank(message = "Rule code is required")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    @NotNull(message = "Rule type is required")
    private RuleType ruleType;

    private String description;

    @NotNull(message = "Rule decision is required")
    private RuleDecision decision;

    private Integer priority = 100;

    /*
     * Examples:
     * 1
     * 1 AND 2
     * (1 AND 2) OR 3
     *
     * If empty, all conditions are evaluated using AND.
     */
    private String expression;

    private Boolean isActive = true;

    /*
     * Do not send from Angular.
     * Controller sets this from X-User-Id gateway header.
     */
    private String requestedBy;

    @NotEmpty(message = "At least one rule condition is required")
    @Valid
    private List<RuleConditionRequest> conditions;
}