package com.bank.service;

import com.bank.dtos.*;
import com.bank.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TransactionService {
    TransactionResponse deposit(AmountTransactionRequest request, HttpServletRequest httpServletRequest);
    TransactionResponse withdraw(AmountTransactionRequest request,HttpServletRequest  httpServletRequest);
    TransactionResponse transfer(TransferRequest request,HttpServletRequest  httpServletRequest);
    List<TransactionResponse> getCustomerTransactions(String customerId);
    List<TransactionResponse> getAccountTransactions(String accountNumber);
    Long getTransactionCount(String customerId);
    List<TransactionResponse> getRecentTransactions(String customerId);
    List<TransactionResponse> getAllTransactions();

    Long countTodayTransactions();

    List<Object[]> getMonthlyStats();


    List<StatementTransactionResponse>
    findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(String accountNumber,LocalDate fromDate,LocalDate toDate);

    TransactionResponse approvePendingTransaction(
            String transactionId,
            String checkerId,
            String remarks,
            HttpServletRequest  httpServletRequest
    );

    TransactionResponse rejectPendingTransaction(
            String transactionId,
            String checkerId,
            String remarks,
            HttpServletRequest  httpServletRequest
    );
    List<TransactionResponse> getPendingApprovalTransactions();

    List<TransactionResponse> getReversalRequiredTransactions();

    TransactionDashboardResponse getDashboardStats();

    List<TransactionDashResponse> getRecentTransactions(int limit);
}