package com.bank.service.impl;

import com.bank.dtos.TransactionDashResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionReportPdfService {

    private final TransactionReportService reportService;

    public byte[] generatePdf(LocalDate fromDate,LocalDate toDate) {

        try {

            List<TransactionDashResponse>
                    transactions =
                    reportService.getReport(
                            fromDate,
                            toDate);

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            Document document =
                    new Document();

            PdfWriter.getInstance(
                    document,
                    output);

            document.open();

            document.add(
                    new Paragraph(
                            "Transaction Report"));

            document.add(
                    new Paragraph(" "));

            PdfPTable table =
                    new PdfPTable(5);

            table.addCell("Reference");
            table.addCell("Type");
            table.addCell("Amount");
            table.addCell("Status");
            table.addCell("Date");

            for (TransactionDashResponse txn :
                    transactions) {

                table.addCell(
                        txn.getReferenceNumber());

                table.addCell(
                        txn.getTransactionType());

                table.addCell(
                        txn.getAmount()
                                .toString());

                table.addCell(
                        txn.getStatus());

                table.addCell(
                        txn.getTransactionDate()
                                .toString());
            }

            document.add(table);

            document.close();

            return output.toByteArray();

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Failed to generate PDF",
                    ex);
        }
    }
}
