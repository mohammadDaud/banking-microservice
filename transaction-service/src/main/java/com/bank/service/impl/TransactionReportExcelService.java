package com.bank.service.impl;

import com.bank.dtos.TransactionDashResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionReportExcelService {

    private final TransactionReportService reportService;

    public byte[] generateExcel(LocalDate fromDate,LocalDate toDate) {
        try {
            List<TransactionDashResponse> transactions = reportService.getReport(fromDate,toDate);
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Transactions");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Reference");
            header.createCell(1).setCellValue("Type");
            header.createCell(2).setCellValue("Amount");
            header.createCell(3).setCellValue("Status");
            header.createCell(4).setCellValue("Date");
            int rowNum = 1;
            for (TransactionDashResponse txn : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(txn.getReferenceNumber());
                row.createCell(1).setCellValue(txn.getTransactionType());
                row.createCell(2).setCellValue(txn.getAmount().doubleValue());
                row.createCell(3).setCellValue(txn.getStatus());
                row.createCell(4).setCellValue(txn.getTransactionDate().toString());
            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbook.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate Excel report",ex);
        }
    }
}
