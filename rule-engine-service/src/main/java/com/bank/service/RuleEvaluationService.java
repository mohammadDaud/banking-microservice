package com.bank.service;

import com.bank.dtos.RuleEvaluationRequest;
import com.bank.dtos.RuleEvaluationResponse;

public interface RuleEvaluationService {
    RuleEvaluationResponse evaluate(RuleEvaluationRequest request);
}