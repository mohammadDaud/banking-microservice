package com.bank.service.impl;

import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private final AuditLogRepository repository;

    @Override
    public ByteArrayInputStream exportAuditLogs() {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Audit Logs");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);

            String[] columns = {
                    "Id",
                    "User Id",
                    "Username",
                    "Role",
                    "Module",
                    "Action",
                    "Entity Id",
                    "Entity Type",
                    "Description",
                    "Created At"
            };

            for (int i = 0; i < columns.length; i++) {

                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            List<AuditLog> logs = repository.findAll();

            int rowIdx = 1;

            for (AuditLog log : logs) {

                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getUserId());
                row.createCell(2).setCellValue(log.getUsername());
                row.createCell(3).setCellValue(log.getRole());
                row.createCell(4).setCellValue(log.getModule().name());
                row.createCell(5).setCellValue(log.getAction().name());
                row.createCell(6).setCellValue(log.getEntityId());
                row.createCell(7).setCellValue(log.getEntityType());
                row.createCell(8).setCellValue(log.getDescription());
                row.createCell(9).setCellValue(log.getCreatedAt().toString());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }
}