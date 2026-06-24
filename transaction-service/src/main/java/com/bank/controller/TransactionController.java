package com.bank.controller;

import com.bank.dtos.AmountTransactionRequest;
import com.bank.dtos.StatementTransactionResponse;
import com.bank.dtos.TransactionResponse;
import com.bank.dtos.TransferRequest;
import com.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/deposit")
    public TransactionResponse deposit(@RequestBody AmountTransactionRequest request) {
        return service.deposit(request);
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(@RequestBody AmountTransactionRequest request) {
        return service.withdraw(request);
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @RequestHeader("X-User-Id") String customerId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestBody TransferRequest request) {
        if (roles != null && !roles.contains("ROLE_CUSTOMER")) {
            throw new IllegalStateException("Only customers can create transfers");
        }
        request.setCustomerId(customerId);
        return service.transfer(request);
    }


    @GetMapping("/customer/{customerId}")
    public List<TransactionResponse> getCustomerTransactions(@PathVariable String customerId) {
        return service.getCustomerTransactions(customerId);
    }

    @GetMapping("/account/{accountNumber}")
    public List<TransactionResponse> getAccountTransactions(@PathVariable String accountNumber) {
        return service.getAccountTransactions(accountNumber);
    }

    @GetMapping("/customer/{customerId}/count")
    public Long getCount(@PathVariable String customerId) {
        return service.getTransactionCount(customerId);
    }

    @GetMapping("/customer/{customerId}/recent")
    public List<TransactionResponse> recentTransactions(@PathVariable String customerId) {
        return service.getRecentTransactions(customerId);
    }

    @GetMapping("/statement")
    public List<StatementTransactionResponse> getStatements(
            @RequestParam String accountNumber,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        return service.findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(accountNumber,fromDate,toDate);
    }
}