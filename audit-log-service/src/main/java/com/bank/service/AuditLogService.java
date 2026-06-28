package com.bank.service;

import com.bank.dtos.AuditDashboardResponse;
import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;
import com.bank.dtos.AuditLogSearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AuditLogService {
    void save(AuditLogRequest request);

    List<AuditLogResponse> findAll();

    Page<AuditLogResponse> search(AuditLogSearchRequest request);

    AuditDashboardResponse dashboard();
}
