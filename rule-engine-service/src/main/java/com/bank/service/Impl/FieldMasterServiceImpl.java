package com.bank.service.Impl;

import com.bank.dtos.FieldMasterRequest;
import com.bank.dtos.FieldMasterResponse;
import com.bank.dtos.FieldOperatorMappingRequest;
import com.bank.model.ConditionalOperator;
import com.bank.model.FieldMaster;
import com.bank.model.FieldOperatorMapping;
import com.bank.repository.ConditionalOperatorRepository;
import com.bank.repository.FieldMasterRepository;
import com.bank.repository.FieldOperatorMappingRepository;
import com.bank.service.FieldMasterService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldMasterServiceImpl implements FieldMasterService {

    private final FieldMasterRepository fieldRepository;
    private final ConditionalOperatorRepository operatorRepository;
    private final FieldOperatorMappingRepository mappingRepository;

    @Override
    @Transactional
    public FieldMasterResponse create(FieldMasterRequest request) {

        String fieldName = request.getFieldName().trim();

        if (fieldRepository.existsByFieldName(fieldName)) {
            throw new IllegalArgumentException(
                    "Field already exists: " + fieldName
            );
        }

        LocalDateTime now = LocalDateTime.now();

        FieldMaster field = FieldMaster.builder()
                .fieldName(fieldName)
                .displayName(request.getDisplayName().trim())
                .dataType(request.getDataType())
                .description(request.getDescription())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .createdBy(request.getRequestedBy())
                .updatedBy(request.getRequestedBy())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return map(fieldRepository.save(field));
    }

    @Override
    @Transactional
    public FieldMasterResponse update(
            Long id,
            FieldMasterRequest request) {

        FieldMaster field = getEntity(id);

        field.setDisplayName(request.getDisplayName().trim());
        field.setDataType(request.getDataType());
        field.setDescription(request.getDescription());
        field.setIsActive(
                request.getIsActive() == null || request.getIsActive()
        );
        field.setUpdatedBy(request.getRequestedBy());
        field.setUpdatedAt(LocalDateTime.now());

        return map(fieldRepository.save(field));
    }

    @Override
    @Transactional
    public FieldMasterResponse getById(Long id) {
        return map(getEntity(id));
    }

    @Override
    @Transactional
    public List<FieldMasterResponse> getAll(Boolean activeOnly) {

        List<FieldMaster> fields = Boolean.TRUE.equals(activeOnly)
                ? fieldRepository.findByIsActiveTrueOrderByDisplayNameAsc()
                : fieldRepository.findAll();

        return fields.stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public FieldMasterResponse addOperator(
            Long fieldId,
            FieldOperatorMappingRequest request) {

        FieldMaster field = getEntity(fieldId);

        ConditionalOperator operator = operatorRepository
                .findById(request.getOperatorId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Operator not found: "
                                        + request.getOperatorId()
                        )
                );

        if (!Boolean.TRUE.equals(operator.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot map inactive operator: "
                            + operator.getShortName()
            );
        }

        if (mappingRepository.existsByFieldIdAndOperatorId(
                fieldId,
                operator.getId())) {

            throw new IllegalArgumentException(
                    "Operator is already mapped to this field"
            );
        }

        FieldOperatorMapping mapping = FieldOperatorMapping.builder()
                .field(field)
                .operator(operator)
                .createdAt(LocalDateTime.now())
                .build();

        mappingRepository.save(mapping);

        /*
         * Important:
         * Reload after saving mapping, so allowedOperators
         * contains the newly added operator in API response.
         */
        return map(getEntity(fieldId));
    }

    @Override
    @Transactional
    public FieldMasterResponse removeOperator(
            Long fieldId,
            Long operatorId) {

        getEntity(fieldId);

        if (!mappingRepository.existsByFieldIdAndOperatorId(
                fieldId,
                operatorId)) {

            throw new IllegalArgumentException(
                    "Operator mapping not found"
            );
        }

        mappingRepository.deleteByFieldIdAndOperatorId(
                fieldId,
                operatorId
        );

        return map(getEntity(fieldId));
    }

    private FieldMaster getEntity(Long id) {
        return fieldRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Field not found: " + id
                        )
                );
    }

    private FieldMasterResponse map(FieldMaster field) {

        List<String> allowedOperators = mappingRepository
                .findByFieldId(field.getId())
                .stream()
                .map(mapping -> mapping.getOperator().getShortName())
                .toList();

        return FieldMasterResponse.builder()
                .id(field.getId())
                .fieldName(field.getFieldName())
                .displayName(field.getDisplayName())
                .dataType(field.getDataType())
                .description(field.getDescription())
                .isActive(field.getIsActive())
                .allowedOperators(allowedOperators)
                .createdAt(field.getCreatedAt())
                .updatedAt(field.getUpdatedAt())
                .build();
    }
}