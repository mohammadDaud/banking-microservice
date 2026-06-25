package com.bank.accs.controller;

import com.bank.accs.dtos.*;
import com.bank.accs.model.Account;
import com.bank.accs.service.AccountService;
import com.bank.accs.service.PdfStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;
    private final PdfStatementService pdfService;

    @PostMapping
    public CreateAccountResponse createAccount(@RequestBody CreateAccountRequest request) {
        return service.createAccount(request);
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@PathVariable String accountNumber) {
        return service.getAccount(accountNumber);
    }

    @GetMapping("/customer/{customerId}")
    public List<Account> getCustomerAccounts(@PathVariable String customerId) {
        return service.getCustomerAccounts(customerId);
    }

    @PutMapping("/{accountNumber}/credit")
    public void credit(@PathVariable String accountNumber,@RequestBody AmountRequest request) {
        service.credit(accountNumber,request.getAmount());
    }

    @PutMapping("/{accountNumber}/debit")
    public void debit(@PathVariable String accountNumber,@RequestBody AmountRequest request) {
        service.debit(accountNumber,request.getAmount());
    }

    @GetMapping("/{accountNumber}/balance")
    public BalanceResponse getBalance(@PathVariable String accountNumber) {
        return service.getBalance(accountNumber);
    }

    @PutMapping("/{accountNumber}/freeze")
    public void freezeAccount(@PathVariable String accountNumber) {
        service.freezeAccount(accountNumber);
    }

    @PutMapping("/{accountNumber}/unfreeze")
    public void unfreezeAccount(@PathVariable String accountNumber) {
        service.unfreezeAccount(accountNumber);
    }

    @PutMapping("/{accountNumber}/close")
    public void closeAccount(@PathVariable String accountNumber) {
        service.closeAccount(accountNumber);
    }

    @GetMapping("/customer/{customerId}/summary")
    public AccountSummaryResponse getSummary(@PathVariable String customerId) {
        return service.getSummary(customerId);
    }

    @GetMapping("/{accountNumber}/statement")
    public AccountStatementResponse getStatement(
            @PathVariable
            String accountNumber,
            @RequestParam
            LocalDate fromDate,
            @RequestParam
            LocalDate toDate) {
        return service.getStatement(accountNumber,fromDate,toDate);
    }

    @GetMapping("/{accountNumber}/statement/pdf")
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable
            String accountNumber,
            @RequestParam
            LocalDate fromDate,
            @RequestParam
            LocalDate toDate) {
        byte[] pdf = pdfService.generateStatementPdf(accountNumber,fromDate,toDate);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=statement-"
                                + accountNumber
                                + "-"
                                + fromDate
                                + "-to-"
                                + toDate
                                + ".pdf"
                )
                .body(pdf);
    }

}
