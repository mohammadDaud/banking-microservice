package com.bank.controller;

import com.bank.dtos.RuleEvaluationRequest;
import com.bank.dtos.RuleEvaluationResponse;
import com.bank.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleEvaluationController {

    private final RuleEvaluationService service;

    @PostMapping("/engine/evaluate")
    public RuleEvaluationResponse evaluate(@RequestBody RuleEvaluationRequest request) {
        return service.evaluate(request);
    }
}