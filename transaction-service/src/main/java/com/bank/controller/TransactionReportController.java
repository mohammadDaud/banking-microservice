package com.bank.controller;

import com.bank.dtos.TransactionDashResponse;
import com.bank.dtos.TransactionReportSummaryResponse;
import com.bank.service.impl.TransactionReportExcelService;
import com.bank.service.impl.TransactionReportPdfService;
import com.bank.service.impl.TransactionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionReportController {

    private final TransactionReportService service;
    private final TransactionReportPdfService pdfService;
    private final TransactionReportExcelService excelService;

    @GetMapping("/summary")
    public TransactionReportSummaryResponse summary() {
        return service.getSummary();
    }

    @GetMapping("/report")
    public List<TransactionDashResponse> report(@RequestParam LocalDate fromDate,@RequestParam LocalDate toDate) {
        return service.getReport(fromDate,toDate);
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> downloadPdf(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        byte[] pdf = pdfService.generatePdf(fromDate,toDate);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transactions.pdf")
                .body(pdf);
    }

    @GetMapping("/report/excel")
    public ResponseEntity<byte[]> downloadExcel(@RequestParam LocalDate fromDate,@RequestParam LocalDate toDate) {
        byte[] excel = excelService.generateExcel(fromDate,toDate);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transactions-report.xlsx")
                .body(excel);
    }
}