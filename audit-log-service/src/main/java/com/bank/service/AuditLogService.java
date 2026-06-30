package com.bank.service;

import com.bank.dtos.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AuditLogService {
    void save(AuditLogRequest request);

    List<AuditLogResponse> findAll();

    Page<AuditLogResponse> search(AuditLogSearchRequest request);

    AuditDashboardResponse getDashboardStats();

    List<RecentAuditResponse> getRecentAudits(int limit);
}
