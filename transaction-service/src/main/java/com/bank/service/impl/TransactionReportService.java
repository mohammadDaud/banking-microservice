package com.bank.service.impl;

import com.bank.dtos.TransactionDashResponse;
import com.bank.dtos.TransactionReportSummaryResponse;
import com.bank.enums.TransactionStatus;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionReportService {
    private final TransactionRepository repository;

    public TransactionReportSummaryResponse getSummary() {

        LocalDate today =LocalDate.now();
        LocalDate firstDay =today.withDayOfMonth(1);
        return TransactionReportSummaryResponse.builder()
                .todayTransactions(repository.countByTransactionDateBetween(
                                today.atStartOfDay(),
                                today.plusDays(1)
                                        .atStartOfDay()))

                .todayAmount(repository.getTotalAmount(
                                today.atStartOfDay(),
                                today.plusDays(1)
                                        .atStartOfDay()))

                .monthlyTransactions(repository.countByTransactionDateBetween(
                                firstDay.atStartOfDay(),
                                firstDay.plusMonths(1)
                                        .atStartOfDay()))

                .monthlyAmount(repository.getTotalAmount(
                                firstDay.atStartOfDay(),
                                firstDay.plusMonths(1)
                                        .atStartOfDay()))

                .successTransactions(repository.countByTransactionStatus(
                                TransactionStatus.SUCCESS))

                .failedTransactions(repository.countByTransactionStatus(
                                TransactionStatus.FAILED))
                .build();
    }

    public List<TransactionDashResponse> getReport(
            LocalDate fromDate,
            LocalDate toDate) {

        return repository
                .findByTransactionDateBetweenOrderByTransactionDateDesc(
                        fromDate.atStartOfDay(),
                        toDate.plusDays(1)
                                .atStartOfDay())
                .stream()
                .map(txn -> TransactionDashResponse
                        .builder()
                        .id(txn.getId())
                        .referenceNumber(
                                txn.getTransactionReference())
                        .amount(txn.getAmount())
                        .transactionType(
                                txn.getTransactionType().name())
                        .status(
                                txn.getTransactionStatus().name())
                        .transactionDate(
                                txn.getTransactionDate())
                        .build())
                .toList();
    }
}
