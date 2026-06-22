package com.bank.service;

import com.bank.dtos.ConditionalOperatorRequest;
import com.bank.dtos.ConditionalOperatorResponse;

import java.util.List;

public interface ConditionalOperatorService {

    ConditionalOperatorResponse create(ConditionalOperatorRequest request);

    ConditionalOperatorResponse update(Long id, ConditionalOperatorRequest request);

    ConditionalOperatorResponse getById(Long id);

    List<ConditionalOperatorResponse> getAll(Boolean activeOnly);
}
