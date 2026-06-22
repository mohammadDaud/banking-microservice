package com.bank.client;

import com.bank.dtos.RuleEvaluationRequest;
import com.bank.dtos.RuleEvaluationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rule-engine-service")
public interface RuleEngineClient {

    @PostMapping("/api/rule/engine/evaluate")
    RuleEvaluationResponse evaluate(@RequestBody RuleEvaluationRequest request);
}