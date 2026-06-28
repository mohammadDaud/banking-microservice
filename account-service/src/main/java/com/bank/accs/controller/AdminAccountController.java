package com.bank.accs.controller;

import com.bank.accs.dtos.AccountDashResponse;
import com.bank.accs.dtos.AccountDashboardResponse;
import com.bank.accs.dtos.AccountResponse;
import com.bank.accs.dtos.AccountTypeStatResponse;
import com.bank.accs.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AccountService service;

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return service.findAllAccounts();
    }

    @PutMapping("/{accountNumber}/freeze")
    public void freeze(@PathVariable String accountNumber, HttpServletRequest httpServletRequest) {
        service.freezeAccount(accountNumber,httpServletRequest);
    }

    @PutMapping("/{accountNumber}/unfreeze")
    public void unfreeze(@PathVariable String accountNumber, HttpServletRequest httpServletRequest) {
        service.unfreezeAccount(accountNumber,httpServletRequest);
    }

    @PutMapping("/{accountNumber}/close")
    public void close(@PathVariable String accountNumber, HttpServletRequest httpServletRequest) {
        service.closeAccount(accountNumber,httpServletRequest);
    }

    @GetMapping("/count")
    public Long count() {
        return service.count();
    }

    @GetMapping("/active-count")
    public Long activeCount() {
        return service.countByStatus("ACTIVE");
    }

    @GetMapping("/type-stats")
    public List<AccountTypeStatResponse> typeStats() {

        return service.getAccountTypeStats()
                .stream()
                .map(record -> AccountTypeStatResponse.builder()
                        .accountType((String) record[0])
                        .count((Long) record[1])
                        .build())
                .toList();
    }

    @GetMapping("/recent")
    public List<AccountDashResponse> recentAccounts() {

        return service
                .findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, 10))
                .stream()
                .map(account -> AccountDashResponse.builder()
                        .id(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getAccountType().name())
                        .status(account.getAccountStatus().name())
                        .createdAt(account.getCreatedAt())
                        .build())
                .toList();
    }

    @GetMapping("/dashboard/stats")
    public AccountDashboardResponse dashboardStats() {
        return service.getDashboardStats();
    }
}