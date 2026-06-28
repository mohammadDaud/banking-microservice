package com.bank.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

public class PdfPageEvent extends PdfPageEventHelper {

    @Override
    public void onEndPage(
            PdfWriter writer,
            Document document) {

        Font font = FontFactory.getFont(
                FontFactory.HELVETICA,
                9,
                Font.NORMAL
        );

        Phrase footer = new Phrase(
                "Banking Microservice Audit Report | Page "
                        + writer.getPageNumber(),
                font);

        ColumnText.showTextAligned(
                writer.getDirectContent(),
                Element.ALIGN_CENTER,
                footer,
                (document.right() + document.left()) / 2,
                document.bottom() - 20,
                0);
    }
}