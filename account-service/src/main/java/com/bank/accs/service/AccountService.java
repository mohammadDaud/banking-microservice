package com.bank.accs.service;

import com.bank.accs.client.AccountNumberClient;
import com.bank.accs.client.KycClient;
import com.bank.accs.client.NotificationClient;
import com.bank.accs.client.TransactionClient;
import com.bank.accs.dtos.*;
import com.bank.accs.exception.BadRequestException;
import com.bank.accs.exception.ResourceNotFoundException;
import com.bank.accs.kafka.KafkaEventPublisher;
import com.bank.accs.model.Account;
import com.bank.accs.model.TransactionLimit;
import com.bank.accs.model.enums.AccountStatus;
import com.bank.accs.model.enums.AccountType;
import com.bank.accs.repository.AccountRepository;
import com.bank.accs.repository.TransactionLimitRepository;
import com.bank.accs.util.IpUtil;
import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import com.bank.common.events.AuditEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.common.util.CorrelationIdUtil;
import com.bank.common.util.EventMetadataUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final KycClient kycClient;
    private final TransactionLimitRepository transactionLimitRepository;

    private static final String SERVICE_NAME = "account-service";

    @Transactional
    public CreateAccountResponse createAccount(CreateAccountRequest request,HttpServletRequest httpServletRequest) {

        KycEligibilityResponse kycEligibility =
                kycClient.checkEligibility(request.getCustomerId());

        if (kycEligibility == null || !kycEligibility.isEligible()) {
            throw new BadRequestException(
                    "Account creation blocked: "
                            + (kycEligibility != null
                            ? kycEligibility.getMessage()
                            : "KYC validation failed")
            );
        }

        String accountNumber = generateAccountNumber();
        BigDecimal openingBalance = request.getOpeningBalance() == null
                ? BigDecimal.ZERO
                : request.getOpeningBalance();

        LocalDateTime now = LocalDateTime.now();

        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .customerId(request.getCustomerId())
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .branchCode(request.getBranchCode())
                .accountStatus(AccountStatus.ACTIVE)
                .availableBalance(openingBalance)
                .ledgerBalance(openingBalance)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account savedAccount = repository.save(account);

        createDefaultTransactionLimit(savedAccount);

        publishAccountAudit(
                account,
                "ACCOUNT_CREATED",
                "Account created successfully",
                httpServletRequest
        );

        sendNotification(
                NotificationRequest.builder()
                        .userId(savedAccount.getCustomerId())
                        .title("Account Created")
                        .message(
                                "Account " + savedAccount.getAccountNumber()
                                        + " created successfully"
                        )
                        .build()
        );

        return CreateAccountResponse.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .customerId(savedAccount.getCustomerId())
                .accountStatus(savedAccount.getAccountStatus().name())
                .build();
    }

    @Transactional
    public void credit(String accountNumber, BigDecimal amount,HttpServletRequest httpServletRequest) {

        validateAmount(amount);

        Account account = repository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber
                ));

        validateAccountCanReceiveMoney(account);

        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLedgerBalance(account.getLedgerBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());

        repository.save(account);


        publishAccountAudit(
                account,
                "CREDIT_ACCOUNT",
                "Account credited by ₹" + amount,
                httpServletRequest
        );
    }

    @Transactional
    public void debit(String accountNumber, BigDecimal amount,HttpServletRequest httpServletRequest) {

        validateAmount(amount);

        Account account = repository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber
                ));

        validateAccountCanSendMoney(account);

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance");
        }

        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setLedgerBalance(account.getLedgerBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());

        repository.save(account);

        publishAccountAudit(
                account,
                "DEBIT_ACCOUNT",
                "Account credited by ₹" + amount,
                httpServletRequest
        );
    }

    public BalanceResponse getBalance(String accountNumber) {
        Account account = repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber
                ));

        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .availableBalance(account.getAvailableBalance())
                .ledgerBalance(account.getLedgerBalance())
                .build();
    }

    public AccountResponse getAccount(String accountNumber) {
        return map(repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found"
                )));
    }

    public List<Account> getCustomerAccounts(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Transactional
    public void freezeAccount(String accountNumber,HttpServletRequest httpServletRequest) {

        Account account = getAccountEntity(accountNumber);
        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException("Closed account cannot be frozen.");
        }
        account.setAccountStatus(AccountStatus.FROZEN);
        account.setUpdatedAt(LocalDateTime.now());

        repository.save(account);


        publishAccountAudit(
                account,
                "ACCOUNT_FREEZE",
                "Account " + account.getAccountNumber() + " has been frozen",
                httpServletRequest
        );

        sendNotification(
                NotificationRequest.builder()
                        .userId(account.getCustomerId())
                        .title("Account Frozen")
                        .message(
                                "Account " + account.getAccountNumber()
                                        + " has been frozen"
                        )
                        .build()
        );
    }

    @Transactional
    public void unfreezeAccount(String accountNumber,HttpServletRequest httpServletRequest) {
        Account account = getAccountEntity(accountNumber);
        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException("Closed account cannot be activated.");
        }
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setUpdatedAt(LocalDateTime.now());

        repository.save(account);


        publishAccountAudit(
                account,
                "ACCOUNT_UNFREEZE",
                "Account " + account.getAccountNumber()
                        + " activated successfully",
                httpServletRequest
        );


        sendNotification(
                NotificationRequest.builder()
                        .userId(account.getCustomerId())
                        .title("Account Activated")
                        .message(
                                "Account " + account.getAccountNumber()
                                        + " activated successfully"
                        )
                        .build()
        );
    }

    @Transactional
    public void closeAccount(String accountNumber,HttpServletRequest httpServletRequest) {
        Account account = getAccountEntity(accountNumber);

        if (account.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Account balance must be zero before closing.");
        }

        if (account.getLedgerBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Ledger balance must be zero before closing.");
        }

        account.setAccountStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());

        repository.save(account);


        publishAccountAudit(
                account,
                "ACCOUNT_CLOSED",
                "Account " + account.getAccountNumber() + " has been closed",
                httpServletRequest
        );
        sendNotification(
                NotificationRequest.builder()
                        .userId(account.getCustomerId())
                        .title("Account Closed")
                        .message(
                                "Account " + account.getAccountNumber()
                                        + " has been closed"
                        )
                        .build()
        );
    }

    public AccountSummaryResponse getSummary(String customerId) {
        List<Account> accounts = repository.findByCustomerId(customerId);

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AccountSummaryResponse.builder()
                .totalAccounts(accounts.size())
                .totalBalance(totalBalance)
                .build();
    }

    public List<AccountResponse> findAllAccounts() {
        return repository.findAll().stream()
                .map(this::map)
                .toList();
    }

    public Long count() {
        return repository.count();
    }

    public Long countByStatus(AccountStatus status) {
        return repository.countByAccountStatus(status);
    }

    public List<Object[]> getAccountTypeStats() {
        return repository.getAccountTypeStats();
    }

    public List<Account> findAllByOrderByCreatedAtDesc(PageRequest pageRequest) {
        return repository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize()
                )
        );
    }

    public AccountStatementResponse getStatement(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate) {

        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BadRequestException("Invalid statement date range");
        }

        Account account = getAccountEntity(accountNumber);

        List<StatementTransactionResponse> statements =
                transactionClient.getStatements(
                        accountNumber,
                        fromDate,
                        toDate
                );

        return AccountStatementResponse.builder()
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .availableBalance(account.getAvailableBalance())
                .ledgerBalance(account.getLedgerBalance())
                .transactions(statements)
                .build();
    }

    public AccountDashboardResponse getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);
        return AccountDashboardResponse.builder()
                .totalAccounts(repository.count())
                .activeAccounts(repository.countByAccountStatus(AccountStatus.ACTIVE))
                .inactiveAccounts(repository.countByAccountStatus(AccountStatus.INACTIVE))
                .savingsAccounts(repository.countByAccountType(AccountType.SAVINGS))
                .currentAccounts(repository.countByAccountType(AccountType.CURRENT))
                .totalBankBalance(repository.getTotalBankBalance())
                .averageAccountBalance(repository.getAverageAccountBalance())
                .accountsCreatedToday(repository.countByCreatedAtBetween(start,end))
                .build();
    }

    @CircuitBreaker(
            name = "accountnumbergen-service",
            fallbackMethod = "accountNumberFallback"
    )
    @Retry(
            name = "accountnumbergen-service",
            fallbackMethod = "accountNumberFallback"
    )
    public String generateAccountNumber() {
        return accountNumberClient.generateAccountNumber().getAccountNumber();
    }

    @CircuitBreaker(
            name = "notificationService",
            fallbackMethod = "notificationFallback"
    )
    @Retry(
            name = "notificationService",
            fallbackMethod = "notificationFallback"
    )
    public void sendNotification(NotificationRequest request) {
        notificationClient.createNotification(request);
    }

    public String accountNumberFallback(Exception ex) {
        log.error("Account Number Service unavailable", ex);
        throw new BadRequestException("Unable to generate account number");
    }

    public void notificationFallback(
            NotificationRequest request,
            Exception ex) {

        log.error("Notification Service unavailable", ex);
    }

    private Account getAccountEntity(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber
                ));
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

    private void createDefaultTransactionLimit(Account account) {
        TransactionLimit transactionLimit = TransactionLimit.builder()
                .id(UUID.randomUUID().toString())
                .customerId(account.getCustomerId())
                .accountNumber(account.getAccountNumber())
                .perTransactionLimit(new BigDecimal("10000.00"))
                .dailyLimit(new BigDecimal("50000.00"))
                .monthlyLimit(new BigDecimal("500000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        transactionLimitRepository.save(transactionLimit);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }

    private void validateAccountCanSendMoney(Account account) {
        switch (account.getAccountStatus()) {

            case CLOSED -> throw new BadRequestException("Cannot debit a closed account.");

            case FROZEN -> throw new BadRequestException("Cannot debit a frozen account.");

            case BLOCKED -> throw new BadRequestException("Cannot debit a blocked account.");

            case DORMANT -> throw new BadRequestException("Cannot debit a dormant account.");

            case CLOSING -> throw new BadRequestException("Account is under closure process.");

            default -> {
            }
        }
    }

    private void validateAccountCanReceiveMoney(Account account) {
        switch (account.getAccountStatus()) {

            case CLOSED -> throw new BadRequestException("Cannot credit a closed account.");

            case BLOCKED -> throw new BadRequestException("Cannot credit a blocked account.");

            case CLOSING -> throw new BadRequestException("Cannot credit an account under closure.");

            default -> {
            }
        }
    }

    private void publishAccountAudit(
            Account account,
            String action,
            String description,
            HttpServletRequest request) {

        EventMetadata metadata = createEventMetadata();

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()

                        // Event Metadata
                        .eventId(EventMetadataUtil.eventId())
                        .correlationId(metadata.getCorrelationId())
                        .requestId(metadata.getRequestId())
                        .serviceName(SERVICE_NAME)
                        .source(EventSource.ACCOUNT_SERVICE)

                        // Request Information
                        .requestUri(request != null ? request.getRequestURI() : null)
                        .requestMethod(request != null ? request.getMethod() : null)
                        .ipAddress(request != null
                                ? IpUtil.getClientIp(request)
                                : "SYSTEM")

                        // User Information
                        .userId(account.getCustomerId())
                        .username(account.getAccountNumber())
                        .role("ROLE_CUSTOMER")

                        // Audit Information
                        .module("ACCOUNT")
                        .action(action)
                        .entityId(account.getId())
                        .entityType("ACCOUNT")
                        .description(description)
                        .status(EventStatus.SUCCESS)

                        // Timestamp
                        .createdAt(metadata.getCreatedAt())

                        .build()
        );
    }

    private EventMetadata createEventMetadata() {

        return EventMetadata.builder()
                .correlationId(CorrelationIdUtil.getCorrelationId())
                .requestId(EventMetadataUtil.requestId())
                .createdAt(EventMetadataUtil.createdAt())
                .build();
    }
}