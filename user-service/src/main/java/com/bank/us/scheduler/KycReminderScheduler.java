package com.bank.us.scheduler;

import com.bank.us.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KycReminderScheduler {

    private final KafkaEventPublisher publisher;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendReminders() {
        log.info("KYC reminder started");
        // KYC logic
        log.info("KYC reminder completed");
    }
}