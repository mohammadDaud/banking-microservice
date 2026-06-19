package com.bank.accs.service;

import com.bank.accs.client.AccountNumberClient;
import com.bank.accs.client.NotificationClient;
import com.bank.accs.client.TransactionClient;
import com.bank.accs.dtos.*;
import com.bank.accs.exception.ResourceNotFoundException;
import com.bank.accs.kafka.KafkaEventPublisher;
import com.bank.accs.model.Account;
import com.bank.accs.model.enums.AccountStatus;
import com.bank.accs.repository.AccountRepository;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;

    private final AccountNumberClient accountNumberClient;

    private final NotificationClient notificationClient;

    private final TransactionClient transactionClient;

    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public CreateAccountResponse createAccount(CreateAccountRequest request) {

        String accountNumber =generateAccountNumber();

        Account account =
                Account.builder()
                        .id(UUID.randomUUID().toString())
                        .accountNumber(accountNumber)
                        .customerId(request.getCustomerId())
                        .accountType(request.getAccountType())
                        .currency(request.getCurrency())
                        .branchCode(request.getBranchCode())
                        .accountStatus(AccountStatus.ACTIVE)
                        .availableBalance(
                                request.getOpeningBalance()
                                        == null
                                        ? BigDecimal.ZERO
                                        : request.getOpeningBalance())

                        .ledgerBalance(
                                request.getOpeningBalance()
                                        == null
                                        ? BigDecimal.ZERO
                                        : request.getOpeningBalance())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        Account savedAccount = repository.save(account);
        sendNotification(
                NotificationRequest
                        .builder()
                        .userId(savedAccount.getCustomerId())
                        .title("Account Created")
                        .message("Account " + savedAccount.getAccountNumber()
                                + " created successfully")
                        .build()
        );

        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(savedAccount.getCustomerId())
                        .username(savedAccount.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_CREATED")
                        .entityId(savedAccount.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + savedAccount.getAccountNumber()
                                + " created successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return CreateAccountResponse.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .customerId(savedAccount.getCustomerId())
                .accountStatus(savedAccount.getAccountStatus().name())
                .build();
    }

    public void credit(String accountNumber, BigDecimal amount) {
        Account account = repository.findByAccountNumber(accountNumber)
                .orElseThrow();
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLedgerBalance(account.getLedgerBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_UPDATED")
                        .entityId(account.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + account.getAccountNumber()
                                + " credited "+amount+" successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        repository.save(account);
    }

    public void debit(String accountNumber, BigDecimal amount) {
        Account account =
                repository.findByAccountNumber(accountNumber)
                        .orElseThrow();

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setLedgerBalance(account.getLedgerBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_UPDATED")
                        .entityId(account.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + account.getAccountNumber()
                                + " debited "+amount+" successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        repository.save(account);
    }

    public BalanceResponse getBalance(String accountNumber) {
        Account account =
                repository.findByAccountNumber(accountNumber)
                        .orElseThrow();
        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .availableBalance(account.getAvailableBalance())
                .ledgerBalance(account.getLedgerBalance())
                .build();
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account =
                repository.findByAccountNumber(
                                accountNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"));

        return map(account);
    }

    public List<Account> getCustomerAccounts(String customerId) {
        return repository.findByCustomerId(
                customerId);
    }

    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account =
                repository.findByAccountNumber(
                                accountNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"));

        account.setAccountStatus(AccountStatus.FROZEN);
        account.setUpdatedAt(LocalDateTime.now());
        repository.save(account);
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_FREEZE")
                        .entityId(account.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + account.getAccountNumber()
                                + " has been frozen")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        sendNotification(
                NotificationRequest
                        .builder()
                        .userId(account.getCustomerId())
                        .title("Account Frozen")
                        .message("Account " + account.getAccountNumber()
                                + " has been frozen")
                        .build()
        );
    }

    @Transactional
    public void unfreezeAccount(String accountNumber) {
        Account account =
                repository.findByAccountNumber(
                                accountNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"));
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());
        repository.save(account);
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_UNFREEZE")
                        .entityId(account.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + account.getAccountNumber()
                                + " activated successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        sendNotification(
                NotificationRequest
                        .builder()
                        .userId(account.getCustomerId())
                        .title("Account Activated")
                        .message("Account " + account.getAccountNumber()
                                + " activated successfully")
                        .build()
        );
    }

    @Transactional
    public void closeAccount(String accountNumber) {
        Account account =
                repository.findByAccountNumber(
                                accountNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"));

        account.setAccountStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());
        repository.save(account);
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("Account")
                        .module("ACCOUNT")
                        .action("ACCOUNT_DELETED")
                        .entityId(account.getId())
                        .entityType("Account")
                        .ipAddress("127.0.0.1")
                        .description("Account " + account.getAccountNumber()
                                + " has been closed")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        sendNotification(
                NotificationRequest
                        .builder()
                        .userId(account.getCustomerId())
                        .title("Account Closed")
                        .message("Account " + account.getAccountNumber()
                                + " has been closed")
                        .build()
        );
    }

    public AccountSummaryResponse getSummary(String customerId) {
        List<Account> accounts = repository.findByCustomerId(customerId);
        BigDecimal totalBalance =
                accounts.stream()
                        .map(Account::getAvailableBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AccountSummaryResponse
                .builder()
                .totalAccounts(accounts.size())
                .totalBalance(totalBalance)
                .build();
    }

    public List<AccountResponse> findAllAccounts() {
        return repository.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public Long count() {
        return repository.count();
    }

    public Long countByStatus(String active) {
        return repository.countByAccountStatus(active);
    }

    public List<Object[]> getAccountTypeStats() {
        return repository.getAccountTypeStats();
    }

    public List<Account> findAllByOrderByCreatedAtDesc(PageRequest of) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(of.getPageNumber(), of.getPageSize()));
    }

    public AccountStatementResponse getStatement(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate) {

        Account account =
                repository
                        .findByAccountNumber(accountNumber)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Account not found"));


        List<StatementTransactionResponse> statements =
                transactionClient.getStatements(accountNumber, fromDate, toDate);


        return AccountStatementResponse
                .builder()
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .availableBalance(account.getAvailableBalance())
                .ledgerBalance(account.getLedgerBalance())
                .transactions(statements)
                .build();
    }

    private AccountResponse map(Account account) {

        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .accountStatus(account.getAccountStatus().name())
                .currency(account.getCurrency().name())
                .branchCode(account.getBranchCode())
                .build();
    }

    @CircuitBreaker(name = "accountnumbergen-service",fallbackMethod = "accountNumberFallback")
    @Retry(name = "accountnumbergen-service",fallbackMethod = "accountNumberFallback")
    public String generateAccountNumber() {
        return accountNumberClient.generateAccountNumber().getAccountNumber();
    }
    @CircuitBreaker(name = "notificationService",fallbackMethod = "notificationFallback")
    @Retry(name = "notificationService",fallbackMethod = "notificationFallback")
    public void sendNotification(NotificationRequest request) {
        notificationClient.createNotification(request);
    }

    public String accountNumberFallback(Exception ex) {
        log.error("Account Number Service unavailable",ex);
        throw new RuntimeException("Unable to generate account number");
    }
    public void notificationFallback(NotificationRequest request,Exception ex) {
        log.error("Notification Service unavailable",ex);
    }
}