package com.bank.service;

import com.bank.dtos.RuleRequest;
import com.bank.dtos.RuleResponse;
import com.bank.enums.RuleType;

import java.util.List;

public interface RuleService {

    RuleResponse create(RuleRequest request);

    RuleResponse update(Long id, RuleRequest request);

    RuleResponse getById(Long id);

    List<RuleResponse> getAll(RuleType ruleType, Boolean activeOnly);

    void delete(Long id);

}
