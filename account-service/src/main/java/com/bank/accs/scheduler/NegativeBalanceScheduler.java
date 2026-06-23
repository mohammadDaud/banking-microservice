package com.bank.accs.scheduler;

import com.bank.accs.model.Account;
import com.bank.accs.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NegativeBalanceScheduler {

    private final AccountRepository repository;

    //@Scheduled(cron = "0 0 */6 * * *")
    public void monitorNegativeBalance() {
        List<Account> accounts = repository.findByAvailableBalanceLessThan(BigDecimal.ZERO);
        log.info("Negative balance accounts {}",accounts.size());
    }
}