package com.bank.us.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormantCustomerScheduler {

    @Scheduled(cron = "0 0 3 * * SUN")
    public void findDormantCustomers() {
        log.info("Dormant customer monitoring");
    }
}