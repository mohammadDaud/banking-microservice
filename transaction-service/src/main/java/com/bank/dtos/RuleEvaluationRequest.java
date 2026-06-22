package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RuleEvaluationRequest {

    private String ruleType;

    private Map<String, Object> payload;
}