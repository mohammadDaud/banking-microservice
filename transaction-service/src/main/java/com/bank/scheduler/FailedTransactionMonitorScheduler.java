package com.bank.scheduler;

import com.bank.enums.TransactionStatus;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FailedTransactionMonitorScheduler {

    private final TransactionRepository repository;

    private final KafkaEventPublisher producer;

    @Scheduled(cron = "0 */30 * * * *")
    public void monitorFailedTransactions() {
        long count = repository.countByTransactionStatus(TransactionStatus.FAILED);
        if(count > 0) {
            log.warn("Failed transactions found {}", count);
        }
    }
}