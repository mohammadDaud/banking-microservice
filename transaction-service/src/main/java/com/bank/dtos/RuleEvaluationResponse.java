package com.bank.dtos;

import lombok.Data;

@Data
public class RuleEvaluationResponse {

    private String decision;

    private String matchedRuleCode;

    private String reason;
}