package com.bank.service;

import com.bank.dtos.FieldMasterRequest;
import com.bank.dtos.FieldMasterResponse;
import com.bank.dtos.FieldOperatorMappingRequest;

import java.util.List;

public interface FieldMasterService {
    FieldMasterResponse create(FieldMasterRequest request);

    FieldMasterResponse update(Long id, FieldMasterRequest request);

    FieldMasterResponse getById(Long id);

    List<FieldMasterResponse> getAll(Boolean activeOnly);

    FieldMasterResponse addOperator(Long fieldId, FieldOperatorMappingRequest request);

    FieldMasterResponse removeOperator(Long fieldId, Long operatorId);
}