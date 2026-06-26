package com.bank.service.impl;

import com.bank.client.AccountMoneyClient;
import com.bank.client.AccountQueryClient;
import com.bank.dtos.TransactionLimitResponse;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionLimitValidator {

    private final AccountQueryClient accountClient;

    private final TransactionRepository repository;

    public void validateTransfer(String accountNumber,BigDecimal amount) {
        TransactionLimitResponse limit = accountClient.getLimit(accountNumber);
        validatePerTransaction(limit,amount);
        validateDailyLimit(limit,accountNumber,amount);
        validateMonthlyLimit(limit,accountNumber,amount);
    }

    private void validatePerTransaction(TransactionLimitResponse limit,BigDecimal amount) {
        if (amount.compareTo(limit.getPerTransactionLimit()) > 0) {
            throw new RuntimeException("Per transaction limit exceeded");
        }
    }

    private void validateDailyLimit(TransactionLimitResponse limit,String accountNumber,BigDecimal amount) {
        LocalDate today = LocalDate.now();
        BigDecimal used = repository.getTodayTransferAmount(
                        accountNumber,
                        today.atStartOfDay(),
                        today.plusDays(1)
                                .atStartOfDay());
        if (used.add(amount).compareTo(limit.getDailyLimit()) > 0) {
            throw new RuntimeException("Daily limit exceeded");
        }
    }

    private void validateMonthlyLimit(TransactionLimitResponse limit,String accountNumber,BigDecimal amount) {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = firstDay.plusMonths(1);
        BigDecimal used = repository.getMonthlyTransferAmount(accountNumber,
                        firstDay.atStartOfDay(),nextMonth.atStartOfDay());
        if (used.add(amount).compareTo(limit.getMonthlyLimit()) > 0) {
            throw new RuntimeException("Monthly limit exceeded");
        }
    }
}