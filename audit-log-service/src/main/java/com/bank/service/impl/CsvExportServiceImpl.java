package com.bank.service.impl;

import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvExportServiceImpl implements CsvExportService {

    private final AuditLogRepository repository;

    @Override
    public ByteArrayInputStream exportAuditLogs() {

        List<AuditLog> logs = repository.findAll();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PrintWriter writer = new PrintWriter(out);

        writer.println(
                "Id,UserId,Username,Role,Module,Action,EntityId,EntityType,Description,CreatedAt"
        );

        for (AuditLog log : logs) {

            writer.printf(
                    "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",

                    log.getId(),
                    log.getUserId(),
                    log.getUsername(),
                    log.getRole(),
                    log.getModule(),
                    log.getAction(),
                    log.getEntityId(),
                    log.getEntityType(),
                    log.getDescription(),
                    log.getCreatedAt()
            );
        }

        writer.flush();

        return new ByteArrayInputStream(out.toByteArray());
    }
}