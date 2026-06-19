package com.bank.accs.scheduler;

import com.bank.accs.model.Account;
import com.bank.accs.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactiveAccountScheduler {

    private final AccountRepository repository;

    @Scheduled(cron = "0 0 2 * * SUN")
    public void monitorInactiveAccounts() {
        LocalDateTime cutoff =LocalDateTime.now().minusMonths(6);
        List<Account> accounts = repository.findByUpdatedAtBefore(cutoff);
        log.info("Inactive accounts found {}",accounts.size());
    }
}