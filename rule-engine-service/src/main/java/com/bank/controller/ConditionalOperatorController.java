package com.bank.controller;

import com.bank.dtos.ConditionalOperatorRequest;
import com.bank.dtos.ConditionalOperatorResponse;
import com.bank.service.ConditionalOperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class ConditionalOperatorController {

    private final ConditionalOperatorService service;

    @PostMapping("/operator")
    public ConditionalOperatorResponse create(
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody ConditionalOperatorRequest request) {

        request.setRequestedBy(adminId);
        return service.create(request);
    }

    @PutMapping("/operator/{id}")
    public ConditionalOperatorResponse update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String adminId,
            @Valid @RequestBody ConditionalOperatorRequest request) {

        request.setRequestedBy(adminId);
        return service.update(id, request);
    }

    @GetMapping("/operator/{id}")
    public ConditionalOperatorResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/operator")
    public List<ConditionalOperatorResponse> getAll(
            @RequestParam(defaultValue = "true") Boolean activeOnly) {

        return service.getAll(activeOnly);
    }
}