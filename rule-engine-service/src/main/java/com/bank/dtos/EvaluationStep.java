package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluationStep {

    private Long ruleId;
    private String ruleCode;
    private String ruleName;

    private Long conditionId;
    private String fieldName;
    private String operator;
    private String expectedValue;
    private Object actualValue;

    private Boolean matched;
    private String message;
}