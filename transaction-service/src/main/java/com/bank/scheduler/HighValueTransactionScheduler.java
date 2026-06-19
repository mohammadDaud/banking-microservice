package com.bank.scheduler;

import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HighValueTransactionScheduler {

    private final TransactionRepository repository;

    @Scheduled(cron = "0 */15 * * * *")
    public void monitorHighValueTransactions() {
        BigDecimal limit = new BigDecimal("500000");
        List<Transaction> transactions = repository.findByAmountGreaterThan(limit);
        log.info("High value transactions: {}",transactions.size());
    }
}