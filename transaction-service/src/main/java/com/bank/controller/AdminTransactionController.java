package com.bank.controller;

import com.bank.dtos.*;
import com.bank.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
    public ResponseEntity<List<TransactionDashResponse>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                service.getRecentTransactions(limit)
        );
    }

    /*
     * Maker-checker APIs
     */

    @GetMapping("/pending")
    public List<TransactionResponse> getPendingTransactions() {
        return service.getPendingApprovalTransactions();
    }

    @PutMapping("/{transactionId}/approve")
    public TransactionResponse approveTransaction(
            @PathVariable String transactionId,
            @RequestHeader("X-User-Id") String checkerId,
            @RequestHeader("X-Roles") String roles,
            @RequestBody CheckerActionRequest request, HttpServletRequest httpServletRequest) {
        validateCheckerRole(roles);
        return service.approvePendingTransaction(
                transactionId,
                checkerId,
                request.getRemarks(),
                httpServletRequest
        );
    }

    @PutMapping("/{transactionId}/reject")
    public TransactionResponse rejectTransaction(@PathVariable String transactionId,
                                                 @RequestHeader("X-User-Id") String checkerId,
                                                 @RequestHeader("X-Roles") String roles,
                                                 @RequestBody CheckerActionRequest request,HttpServletRequest  httpServletRequest) {
        validateCheckerRole(roles);

        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new RuntimeException("Remarks are required when rejecting a transaction");
        }
        return service.rejectPendingTransaction(
                transactionId,
                checkerId,
                request.getRemarks(),
                httpServletRequest
        );
    }

    @GetMapping("/reversal-required")
    public List<TransactionResponse> getReversalRequiredTransactions() {
        return service.getReversalRequiredTransactions();
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<TransactionDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(service.getDashboardStats());
    }


    private void validateCheckerRole(String roles) {
        if (roles == null) {
            throw new RuntimeException("User roles are missing");
        }

        boolean allowed = Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role ->
                        "ROLE_ADMIN".equalsIgnoreCase(role)
                                || "ROLE_CHECKER".equalsIgnoreCase(role)
                );

        if (!allowed) {
            throw new RuntimeException(
                    "Only ADMIN or CHECKER can approve or reject transactions"
            );
        }
    }
}