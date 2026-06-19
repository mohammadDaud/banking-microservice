package com.bank.service;

import com.bank.dtos.AmountTransactionRequest;
import com.bank.dtos.StatementTransactionResponse;
import com.bank.dtos.TransactionResponse;
import com.bank.dtos.TransferRequest;
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
    public Long getTransactionCount(String customerId);
    public List<TransactionResponse> getRecentTransactions(String customerId);
    List<TransactionResponse> getAllTransactions();

    Long countTodayTransactions();

    List<Object[]> getMonthlyStats();

   List<Transaction> findAllByOrderByTransactionDateDesc(PageRequest of);

    List<StatementTransactionResponse>
    findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate);
}