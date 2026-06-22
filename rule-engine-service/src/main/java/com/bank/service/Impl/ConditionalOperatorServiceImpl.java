package com.bank.service.Impl;

import com.bank.dtos.ConditionalOperatorRequest;
import com.bank.dtos.ConditionalOperatorResponse;
import com.bank.model.ConditionalOperator;
import com.bank.repository.ConditionalOperatorRepository;
import com.bank.service.ConditionalOperatorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConditionalOperatorServiceImpl implements ConditionalOperatorService {

    private final ConditionalOperatorRepository repository;

    @Override
    @Transactional
    public ConditionalOperatorResponse create(ConditionalOperatorRequest request) {
        if (repository.existsByShortName(request.getShortName())) {
            throw new IllegalArgumentException(
                    "Operator already exists: " + request.getShortName()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        ConditionalOperator operator = ConditionalOperator.builder()
                .shortName(request.getShortName().trim().toUpperCase())
                .symbol(request.getSymbol().trim())
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdBy(request.getRequestedBy())
                .updatedBy(request.getRequestedBy())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return map(repository.save(operator));
    }

    @Override
    @Transactional
    public ConditionalOperatorResponse update(Long id, ConditionalOperatorRequest request) {
        ConditionalOperator operator = getEntity(id);

        operator.setSymbol(request.getSymbol().trim());
        operator.setDisplayName(request.getDisplayName().trim());
        operator.setDescription(request.getDescription());
        operator.setCategory(request.getCategory());
        operator.setIsActive(request.getIsActive() == null || request.getIsActive());
        operator.setUpdatedBy(request.getRequestedBy());
        operator.setUpdatedAt(LocalDateTime.now());

        return map(repository.save(operator));
    }

    @Override
    public ConditionalOperatorResponse getById(Long id) {
        return map(getEntity(id));
    }

    @Override
    public List<ConditionalOperatorResponse> getAll(Boolean activeOnly) {
        List<ConditionalOperator> operators = Boolean.TRUE.equals(activeOnly)
                ? repository.findByIsActiveTrueOrderByDisplayNameAsc()
                : repository.findAll();

        return operators.stream().map(this::map).toList();
    }

    private ConditionalOperator getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operator not found: " + id));
    }

    private ConditionalOperatorResponse map(ConditionalOperator operator) {
        return ConditionalOperatorResponse.builder()
                .id(operator.getId())
                .shortName(operator.getShortName())
                .symbol(operator.getSymbol())
                .displayName(operator.getDisplayName())
                .description(operator.getDescription())
                .category(operator.getCategory())
                .isActive(operator.getIsActive())
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .build();
    }
}