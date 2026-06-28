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

    //@Scheduled(cron = "0 0 1 * * SUN")
    //@Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        repository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted notifications older than {}",cutoff);
    }
}