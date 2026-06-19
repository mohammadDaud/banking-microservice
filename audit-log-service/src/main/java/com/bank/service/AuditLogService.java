package com.bank.service;

import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;

import java.util.List;

public interface AuditLogService {
    void save(AuditLogRequest request);

    List<AuditLogResponse> findAll();
}
