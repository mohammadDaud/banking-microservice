package com.bank.service;

import com.bank.dtos.AuditLogSearchRequest;

import java.io.ByteArrayInputStream;

public interface PdfExportService {

    ByteArrayInputStream exportAuditLogs();
    ByteArrayInputStream export(
            AuditLogSearchRequest request,
            String generatedBy);

}