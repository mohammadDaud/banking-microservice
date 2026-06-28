package com.bank.service;

import java.io.ByteArrayInputStream;

public interface CsvExportService {
    ByteArrayInputStream exportAuditLogs();

}