package com.bank.controller;

import com.bank.dtos.CheckerActionRequest;
import com.bank.dtos.MonthlyTransactionResponse;
import com.bank.dtos.TransactionDashResponse;
import com.bank.dtos.TransactionResponse;
import com.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final TransactionService service;

    /*
     * Existing admin APIs
     */

    @GetMapping
    public List<TransactionResponse> getAllTransactions() {
        return service.getAllTransactions();
    }

    @GetMapping("/today-count")
    public Long todayCount() {
        return service.countTodayTransactions();
    }

    @GetMapping("/monthly-stats")
    public List<MonthlyTransactionResponse> monthlyStats() {
        return service.getMonthlyStats()
                .stream()
                .map(record -> MonthlyTransactionResponse.builder()
                                .month((String) record[0])
                                .amount(((Number) record[1]).doubleValue())
                                .build()
                )
                .toList();
    }

    @GetMapping("/recent")
    public List<TransactionDashResponse> recentTransactions() {
        return service.findAllByOrderByTransactionDateDesc(PageRequest.of(0, 10))
                .stream()
                .map(txn -> TransactionDashResponse.builder()
                        .id(txn.getId())
                        .referenceNumber(txn.getTransactionReference())
                        .amount(txn.getAmount())
                        .transactionType(txn.getTransactionType().name())
                        .status(txn.getTransactionStatus().name())
                        .transactionDate(txn.getTransactionDate())
                        .build()
                )
                .toList();
    }

    /*
     * Maker-checker APIs
     */

    @GetMapping("/pending")
    public List<TransactionResponse> getPendingTransactions() {
        return service.getPendingApprovalTransactions();
    }

    @PutMapping("/{transactionId}/approve")
    public TransactionResponse approveTransaction(@PathVariable String transactionId, @RequestBody CheckerActionRequest request) {
        return service.approvePendingTransaction(transactionId,request);
    }

    @PutMapping("/{transactionId}/reject")
    public TransactionResponse rejectTransaction(@PathVariable String transactionId, @RequestBody CheckerActionRequest request) {
        return service.rejectPendingTransaction(transactionId,request);
    }
}