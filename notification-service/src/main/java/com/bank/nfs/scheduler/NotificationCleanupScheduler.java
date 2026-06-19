package com.bank.nfs.scheduler;

import com.bank.nfs.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository repository;

    @Scheduled(cron = "0 0 1 * * SUN")
    public void cleanup() {
        repository.deleteByCreatedAtBefore(LocalDateTime.now().minusMonths(6));
        log.info("Old notifications removed");
    }
}