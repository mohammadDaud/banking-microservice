package com.bank.service;

import com.bank.dtos.*;
import com.bank.model.Transaction;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TransactionService {
    TransactionResponse deposit(AmountTransactionRequest request);
    TransactionResponse withdraw(AmountTransactionRequest request);
    TransactionResponse transfer(TransferRequest request);
    List<TransactionResponse> getCustomerTransactions(String customerId);
    List<TransactionResponse> getAccountTransactions(String accountNumber);
    Long getTransactionCount(String customerId);
    List<TransactionResponse> getRecentTransactions(String customerId);
    List<TransactionResponse> getAllTransactions();

    Long countTodayTransactions();

    List<Object[]> getMonthlyStats();

    List<Transaction> findAllByOrderByTransactionDateDesc(PageRequest of);

    List<StatementTransactionResponse>
    findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(String accountNumber,LocalDate fromDate,LocalDate toDate);

    TransactionResponse approvePendingTransaction(
            String transactionId,
            String checkerId,
            String remarks
    );

    TransactionResponse rejectPendingTransaction(
            String transactionId,
            String checkerId,
            String remarks
    );
    List<TransactionResponse> getPendingApprovalTransactions();
}