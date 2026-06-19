package com.bank.accs.service;

import com.bank.accs.client.TransactionClient;
import com.bank.accs.dtos.AccountStatementResponse;
import com.bank.accs.dtos.StatementTransactionResponse;
import com.bank.accs.model.Account;
import com.bank.accs.repository.AccountRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfStatementService {

    private final AccountRepository accountRepository;
    private final TransactionClient transactionClient;

    public byte[] generateStatementPdf(String accountNumber,LocalDate fromDate,LocalDate toDate) {
        try {
            Account account = accountRepository.findByAccountNumber(accountNumber)
                            .orElseThrow(() -> new RuntimeException("Account not found"));

            List<StatementTransactionResponse> statements =
                    transactionClient.getStatements(accountNumber, fromDate, toDate);

            AccountStatementResponse statement = AccountStatementResponse
                    .builder()
                    .accountNumber(account.getAccountNumber())
                    .customerId(account.getCustomerId())
                    .accountType(account.getAccountType().name())
                    .availableBalance(account.getAvailableBalance())
                    .ledgerBalance(account.getLedgerBalance())
                    .transactions(statements)
                    .build();

            ByteArrayOutputStream outputStream =new ByteArrayOutputStream();
            Document document =new Document(PageSize.A4);
            PdfWriter.getInstance(document,outputStream);
            document.open();
            addHeader(document);
            addAccountDetails(document,statement);
            addTransactionTable(document,statement);
            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to generate statement PDF",ex);
        }
    }

    private void addHeader(Document document)throws Exception {
        Font titleFont =new Font(Font.HELVETICA,18,Font.BOLD);
        Paragraph title = new Paragraph("BANK ACCOUNT STATEMENT",titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));
    }

    private void addAccountDetails(Document document,AccountStatementResponse statement)throws Exception {
        document.add(new Paragraph("Account Number : "+ statement.getAccountNumber()));
        document.add(new Paragraph("Customer Id : "+ statement.getCustomerId()));
        document.add(new Paragraph("Account Type : "+ statement.getAccountType()));
        document.add(new Paragraph("Available Balance : ₹"+ statement.getAvailableBalance()));
        document.add(new Paragraph("Ledger Balance : ₹" + statement.getLedgerBalance()));
        document.add(new Paragraph(" "));
    }

    private void addTransactionTable(Document document,AccountStatementResponse statement)throws Exception {
        PdfPTable table =new PdfPTable(6);
        table.setWidthPercentage(100);
        table.addCell("Reference");
        table.addCell("Type");
        table.addCell("Status");
        table.addCell("Amount");
        table.addCell("Source");
        table.addCell("Destination");
        for (StatementTransactionResponse txn : statement.getTransactions()) {
            table.addCell(txn.getTransactionReference());
            table.addCell(txn.getTransactionType());
            table.addCell(txn.getTransactionStatus());
            table.addCell(txn.getAmount().toString());
            table.addCell(txn.getSourceAccount());
            table.addCell(txn.getDestinationAccount());
        }
        document.add(table);
    }
}
