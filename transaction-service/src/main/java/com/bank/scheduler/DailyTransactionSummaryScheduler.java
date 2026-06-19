package com.bank.scheduler;

import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyTransactionSummaryScheduler {

    private final TransactionRepository repository;

    @Scheduled(cron = "0 0 23 * * *")
    public void generateSummary() {
        log.info("Daily transaction summary started");
        long totalTransactions =repository.count();
        log.info("Total transactions today: {}",totalTransactions);
        log.info("Daily transaction summary completed");
    }
}