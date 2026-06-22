package com.bank.controller;

import com.bank.dtos.RuleRequest;
import com.bank.dtos.RuleResponse;
import com.bank.enums.RuleType;
import com.bank.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService service;

    @PostMapping("/rules")
    public RuleResponse create(@RequestBody RuleRequest request) {
        return service.create(request);
    }

    @PutMapping("/rules/{id}")
    public RuleResponse update(@PathVariable Long id,@RequestBody RuleRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/rules/{id}")
    public RuleResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/rules")
    public List<RuleResponse> getAll(@RequestParam(required = false) RuleType ruleType,
            @RequestParam(defaultValue = "true") Boolean activeOnly) {

        return service.getAll(ruleType, activeOnly);
    }

    @DeleteMapping("/rules/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}