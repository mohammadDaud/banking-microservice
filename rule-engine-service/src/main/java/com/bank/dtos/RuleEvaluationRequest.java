package com.bank.dtos;

import com.bank.enums.RuleType;
import lombok.Data;

import java.util.Map;

@Data
public class RuleEvaluationRequest {
    private RuleType ruleType;
    private Map<String, Object> payload;
}