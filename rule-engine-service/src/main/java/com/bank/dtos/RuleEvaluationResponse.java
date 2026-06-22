package com.bank.dtos;

import com.bank.enums.RuleDecision;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RuleEvaluationResponse {

    private RuleDecision decision;

    private String matchedRuleCode;
    private String reason;

    private List<EvaluationStep> evaluationSteps;
}