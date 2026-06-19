package com.bank.controller;

import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;
import com.bank.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService service;

    @PostMapping
    public void create(@RequestBody AuditLogRequest request) {
        service.save(request);
    }

    @GetMapping
    public List<AuditLogResponse> findAll() {
        return service.findAll();
    }
}