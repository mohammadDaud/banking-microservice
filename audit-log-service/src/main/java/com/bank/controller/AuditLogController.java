package com.bank.controller;

import com.bank.dtos.AuditDashboardResponse;
import com.bank.dtos.AuditLogRequest;
import com.bank.dtos.AuditLogResponse;
import com.bank.dtos.AuditLogSearchRequest;
import com.bank.service.AuditLogService;
import com.bank.service.CsvExportService;
import com.bank.service.ExcelExportService;
import com.bank.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogService service;
    private final CsvExportService csvExportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    @PostMapping
    public void create(@RequestBody AuditLogRequest request) {
        service.save(request);
    }

    @GetMapping
    public List<AuditLogResponse> findAll() {
        return service.findAll();
    }

    @PostMapping("/search")
    public Page<AuditLogResponse> search(@RequestBody AuditLogSearchRequest request) {
        return service.search(request);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AuditDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(service.getDashboardStats());
    }

    @GetMapping("/export/csv")
    public ResponseEntity<InputStreamResource> exportCsv() {

        String fileName = "audit_logs.csv";
        InputStreamResource resource =
                new InputStreamResource(csvExportService.exportAuditLogs());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<InputStreamResource> exportExcel() {
        InputStreamResource resource =
                new InputStreamResource(excelExportService.exportAuditLogs());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=audit_logs.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<InputStreamResource> exportPdf() {
        InputStreamResource resource =
                new InputStreamResource(pdfExportService.exportAuditLogs());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=audit_logs.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}