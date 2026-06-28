package com.bank.service.impl;

import com.bank.dtos.AuditLogSearchRequest;
import com.bank.model.AuditLog;
import com.bank.repository.AuditLogRepository;
import com.bank.service.PdfExportService;
import com.bank.service.PdfPageEvent;
import com.bank.specification.AuditLogSpecification;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {

    private final AuditLogRepository repository;

    @Override
    public ByteArrayInputStream exportAuditLogs() {

        try {

            List<AuditLog> logs = repository.findAll();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4.rotate());

            PdfWriter.getInstance(document, out);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    18,
                    Color.BLUE
            );

            Font headerFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    11,
                    Color.WHITE
            );

            Font bodyFont = FontFactory.getFont(
                    FontFactory.HELVETICA,
                    10,
                    Color.BLACK
            );

            Paragraph title = new Paragraph(
                    "Bank Audit Log Report",
                    titleFont
            );

            title.setAlignment(Element.ALIGN_CENTER);

            title.setSpacingAfter(20);

            document.add(title);

            PdfPTable table = new PdfPTable(8);

            table.setWidthPercentage(100);

            table.setWidths(new float[]{
                    2f,
                    2f,
                    2f,
                    2f,
                    2f,
                    3f,
                    5f,
                    3f
            });

            addHeader(table, "User", headerFont);
            addHeader(table, "Role", headerFont);
            addHeader(table, "Module", headerFont);
            addHeader(table, "Action", headerFont);
            addHeader(table, "Entity", headerFont);
            addHeader(table, "Entity Type", headerFont);
            addHeader(table, "Description", headerFont);
            addHeader(table, "Created At", headerFont);

            for (AuditLog log : logs) {

                table.addCell(new Phrase(
                        safe(log.getUsername()),
                        bodyFont));

                table.addCell(new Phrase(
                        safe(log.getRole()),
                        bodyFont));

                table.addCell(new Phrase(
                        log.getModule().name(),
                        bodyFont));

                table.addCell(new Phrase(
                        log.getAction().name(),
                        bodyFont));

                table.addCell(new Phrase(
                        safe(log.getEntityId()),
                        bodyFont));

                table.addCell(new Phrase(
                        safe(log.getEntityType()),
                        bodyFont));

                table.addCell(new Phrase(
                        safe(log.getDescription()),
                        bodyFont));

                table.addCell(new Phrase(
                        log.getCreatedAt().toString(),
                        bodyFont));
            }

            document.add(table);

            document.close();

            return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception ex) {

            throw new RuntimeException(
                    "Unable to generate PDF",
                    ex
            );
        }

    }

    @Override
    public ByteArrayInputStream export(
            AuditLogSearchRequest request,
            String generatedBy) {

        try {

            Pageable pageable = Pageable.unpaged();

            List<AuditLog> logs =
                    repository.findAll(
                                    AuditLogSpecification.search(request),
                                    pageable)
                            .getContent();

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(PageSize.A4.rotate());

            PdfWriter writer =
                    PdfWriter.getInstance(document, out);

            writer.setPageEvent(
                    new PdfPageEvent());

            document.open();

            addHeadersearch(document, generatedBy, logs.size());

            addSearchCriteria(document, request);

            addTable(document, logs);

            document.close();

            return new ByteArrayInputStream(
                    out.toByteArray());

        } catch (Exception ex) {

            throw new RuntimeException(ex);

        }
    }

    private void addTable(
            Document document,
            List<AuditLog> logs) throws Exception {

        PdfPTable table = new PdfPTable(8);

        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                2f,
                2f,
                2f,
                2f,
                2f,
                2f,
                5f,
                3f
        });

        Font headerFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                10,
                Color.WHITE
        );

        addTableHeader(table, "User", headerFont);
        addTableHeader(table, "Role", headerFont);
        addTableHeader(table, "Module", headerFont);
        addTableHeader(table, "Action", headerFont);
        addTableHeader(table, "Entity", headerFont);
        addTableHeader(table, "Entity Type", headerFont);
        addTableHeader(table, "Description", headerFont);
        addTableHeader(table, "Created At", headerFont);

        Font bodyFont = FontFactory.getFont(
                FontFactory.HELVETICA,
                9
        );

        int row = 0;

        for (AuditLog log : logs) {

            Color bg =
                    row % 2 == 0
                            ? Color.WHITE
                            : new Color(245, 245, 245);

            addBodyCell(table, safe(log.getUsername()), bodyFont, bg);
            addBodyCell(table, safe(log.getRole()), bodyFont, bg);
            addBodyCell(table, log.getModule().name(), bodyFont, bg);
            addBodyCell(table, log.getAction().name(), bodyFont, bg);
            addBodyCell(table, safe(log.getEntityId()), bodyFont, bg);
            addBodyCell(table, safe(log.getEntityType()), bodyFont, bg);
            addBodyCell(table, safe(log.getDescription()), bodyFont, bg);
            addBodyCell(table, log.getCreatedAt().toString(), bodyFont, bg);

            row++;
        }

        document.add(table);
    }

    private void addTableHeader(
            PdfPTable table,
            String title,
            Font font) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(title, font));

        cell.setBackgroundColor(
                Color.DARK_GRAY);

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER);

        cell.setPadding(8);

        table.addCell(cell);
    }

    private void addBodyCell(
            PdfPTable table,
            String value,
            Font font,
            Color color) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                value == null ? "" : value,
                                font));

        cell.setBackgroundColor(color);

        cell.setPadding(6);

        table.addCell(cell);
    }

    private void addHeadersearch(
            Document document,
            String generatedBy,
            int totalRecords) throws Exception {

        Font titleFont = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD,
                20,
                Color.BLUE
        );

        Paragraph title = new Paragraph(
                "BANK AUDIT REPORT",
                titleFont
        );

        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);

        document.add(title);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        addInfoCell(infoTable, "Generated By");
        addInfoValue(infoTable, generatedBy);

        addInfoCell(infoTable, "Generated On");
        addInfoValue(infoTable, LocalDateTime.now().toString());

        addInfoCell(infoTable, "Total Records");
        addInfoValue(infoTable, String.valueOf(totalRecords));

        document.add(infoTable);

        document.add(new Paragraph(" "));
    }

    private void addInfoCell(
            PdfPTable table,
            String text) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                FontFactory.getFont(
                                        FontFactory.HELVETICA_BOLD)));

        cell.setBackgroundColor(
                new Color(220,220,220));

        table.addCell(cell);
    }

    private void addInfoValue(
            PdfPTable table,
            String value) {

        table.addCell(
                value == null ? "" : value
        );
    }

    private void addSearchCriteria(
            Document document,
            AuditLogSearchRequest request)
            throws Exception {

        Paragraph p =
                new Paragraph(
                        "Search Criteria");

        p.setSpacingAfter(10);

        document.add(p);

        PdfPTable table =
                new PdfPTable(2);

        table.setWidthPercentage(100);

        addCriteria(
                table,
                "User",
                request.getUsername());

        addCriteria(
                table,
                "Module",
                String.valueOf(
                        request.getModule()));

        addCriteria(
                table,
                "Action",
                String.valueOf(
                        request.getAction()));

        addCriteria(
                table,
                "From",
                String.valueOf(
                        request.getFromDate()));

        addCriteria(
                table,
                "To",
                String.valueOf(
                        request.getToDate()));

        document.add(table);

    }
    private void addCriteria(
            PdfPTable table,
            String key,
            String value) {

        PdfPCell keyCell = new PdfPCell(new Phrase(key));

        keyCell.setBackgroundColor(new Color(230,230,230));

        table.addCell(keyCell);

        table.addCell(
                value == null ? "" : value
        );
    }

    private void addHeader(
            PdfPTable table,
            String title,
            Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(title, font));

        cell.setBackgroundColor(Color.DARK_GRAY);

        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        cell.setPadding(8);

        table.addCell(cell);
    }

    private String safe(String value) {

        return value == null ? "" : value;

    }

}