package com.bank.controller;

import com.bank.dtos.FieldMasterRequest;
import com.bank.dtos.FieldMasterResponse;
import com.bank.dtos.FieldOperatorMappingRequest;
import com.bank.service.FieldMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class FieldMasterController {

    private final FieldMasterService service;

    @PostMapping("/fields")
    public FieldMasterResponse create(
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody FieldMasterRequest request) {

        request.setRequestedBy(adminId);
        return service.create(request);
    }

    @PutMapping("/fields/{id}")
    public FieldMasterResponse update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody FieldMasterRequest request) {

        request.setRequestedBy(adminId);
        return service.update(id, request);
    }

    @GetMapping("/fields/{id}")
    public FieldMasterResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/fields")
    public List<FieldMasterResponse> getAll(
            @RequestParam(defaultValue = "true") Boolean activeOnly) {

        return service.getAll(activeOnly);
    }

    @PostMapping("/fields/{fieldId}/operators")
    public FieldMasterResponse addOperator(
            @PathVariable Long fieldId,
            @Valid @RequestBody FieldOperatorMappingRequest request) {

        return service.addOperator(fieldId, request);
    }

    @DeleteMapping("/fields/{fieldId}/operators/{operatorId}")
    public FieldMasterResponse removeOperator(
            @PathVariable Long fieldId,
            @PathVariable Long operatorId) {

        return service.removeOperator(fieldId, operatorId);
    }
}