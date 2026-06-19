package com.bank.scheduler;

import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyStatementScheduler {

    private final TransactionRepository repository;

    @Scheduled(cron = "0 0 1 1 * *")
    public void generateMonthlyStatements() {
        log.info("Monthly statement generation started");
        // PDF generation logic
        log.info("Monthly statement generation completed");
    }
}