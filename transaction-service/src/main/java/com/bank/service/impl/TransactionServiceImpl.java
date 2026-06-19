package com.bank.service.impl;

import com.bank.client.AccountClient;
import com.bank.client.NotificationClient;
import com.bank.client.TransactionReferenceClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.*;
import com.bank.enums.TransactionStatus;
import com.bank.enums.TransactionType;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import com.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final TransactionReferenceClient transactionReferenceClient;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher  kafkaEventPublisher;
    private final TransactionLimitValidator limitValidator;


    @Override
    public TransactionResponse deposit(AmountTransactionRequest request) {
        AmountRequest amountRequest = new AmountRequest();
        amountRequest.setAmount(request.getAmount());
        accountClient.credit(request.getAccountNumber(), amountRequest);
        Transaction transaction =
                saveTransaction(
                        request.getCustomerId(),
                        request.getAccountNumber(),
                        null,
                        request.getAmount(),
                        TransactionType.DEPOSIT,
                        request.getDescription());
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(request.getCustomerId())
                        .title("Deposit Successful")
                        .message("₹ "+ request.getAmount()+ " deposited successfully")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(transaction.getCustomerId())
                        .module("TRANSACTION")
                        .action("DEPOSIT_SUCCESS")
                        .entityId(transaction.getId())
                        .entityType("TRANSACTION")
                        .ipAddress("127.0.0.1")
                        .description("Deposit ₹" + transaction.getAmount())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        return map(transaction);

    }

    @Override
    public TransactionResponse withdraw(AmountTransactionRequest request) {
        AmountRequest amountRequest = new AmountRequest();
        amountRequest.setAmount(request.getAmount());
        accountClient.debit(request.getAccountNumber(), amountRequest);

        Transaction transaction =
                saveTransaction(
                        request.getCustomerId(),
                        request.getAccountNumber(),
                        null,
                        request.getAmount(),
                        TransactionType.WITHDRAW,
                        request.getDescription());
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(request.getCustomerId())
                        .title("Withdrawal Successful")
                        .message("₹ "+ request.getAmount()+ " withdrawn successfully")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(transaction.getCustomerId())
                        .module("TRANSACTION")
                        .action("WITHDRAW_SUCCESS")
                        .entityId(transaction.getId())
                        .entityType("TRANSACTION")
                        .ipAddress("127.0.0.1")
                        .description("Withdrawal ₹" + transaction.getAmount()+ " withdrawn successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return map(transaction);
    }

    @Override
    public TransactionResponse transfer(TransferRequest request) {
        limitValidator.validateTransfer(
                request.getFromAccount(),
                request.getAmount());
        if(request.getFromAccount().equals(request.getToAccount())) {
            throw new RuntimeException("Cannot transfer to same account");
        }
        AmountRequest debitRequest = new AmountRequest();
        debitRequest.setAmount(request.getAmount());
        accountClient.debit(request.getFromAccount(), debitRequest);
        AmountRequest creditRequest = new AmountRequest();
        creditRequest.setAmount(request.getAmount());
        accountClient.credit(request.getToAccount(), creditRequest);
        Transaction transaction =
                saveTransaction(
                        request.getCustomerId(),
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        TransactionType.TRANSFER,
                        request.getDescription());

        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(request.getCustomerId())
                        .title("Transfer Successful")
                        .message("₹ "+ request.getAmount()+ " transferred successfully")
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(transaction.getCustomerId())
                        .module("TRANSACTION")
                        .action("TRANSFER_SUCCESS")
                        .entityId(transaction.getId())
                        .entityType("TRANSACTION")
                        .ipAddress("127.0.0.1")
                        .description("Transfer ₹" + transaction.getAmount()+ " transferred successfully")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
        kafkaEventPublisher.publish(KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(transaction.getCustomerId())
                        .title("Transaction Successful")
                        .message("₹" + transaction.getAmount() + " transferred successfully")
                        .type("TRANSACTION")
                        .priority("MEDIUM")
                        .build()
        );
        return map(transaction);
    }

    @Override
    public List<TransactionResponse> getCustomerTransactions(String customerId) {
        return repository
                .findByCustomerIdOrderByTransactionDateDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        return repository
                .findBySourceAccountOrderByTransactionDateDesc(accountNumber)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long getTransactionCount(String customerId) {
        return repository.countByCustomerId(customerId);
    }

    @Override
    public List<TransactionResponse> getRecentTransactions(String customerId) {
        return repository.findTop5ByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<TransactionResponse> getAllTransactions() {
        return repository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Long countTodayTransactions() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return repository
                .countTodayTransactions(
                        start,
                        end);
    }

    @Override
    public List<Object[]> getMonthlyStats() {
        return repository.getMonthlyStats();
    }

    @Override
    public List<Transaction> findAllByOrderByTransactionDateDesc(PageRequest of) {
        return repository.findAllByOrderByTransactionDateDesc(PageRequest.of(of.getPageNumber(),of.getPageSize()));
    }

    @Override
    public List<StatementTransactionResponse> findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(String accountNumber, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start =
                fromDate.atStartOfDay();

        LocalDateTime end =
                toDate.plusDays(1)
                        .atStartOfDay();

        var transactions =
                repository
                        .findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
                                accountNumber,
                                start,
                                end)
                        .stream()
                        .map(this::mapTransaction)
                        .toList();

        return transactions;
    }

    private Transaction saveTransaction(
            String customerId,
            String sourceAccount,
            String destinationAccount,
            java.math.BigDecimal amount,
            TransactionType type,
            String description) {

        String referenceNumber =
                transactionReferenceClient
                        .generateTransactionReference()
                        .getReferenceNumber();

        Transaction transaction =
                Transaction.builder()
                        .id(UUID.randomUUID().toString())
                        .transactionReference(referenceNumber)
                        .customerId(customerId)
                        .sourceAccount(sourceAccount)
                        .destinationAccount(destinationAccount)
                        .transactionType(type)
                        .transactionStatus(TransactionStatus.SUCCESS)
                        .amount(amount)
                        .description(description)
                        .transactionDate(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();

        return repository.save(
                transaction);
    }

    private TransactionResponse map(
            Transaction transaction) {
        return TransactionResponse
                .builder()
                .transactionReference(transaction.getTransactionReference())
                .sourceAccount(transaction.getSourceAccount())
                .destinationAccount(transaction.getDestinationAccount())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(transaction.getTransactionStatus())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }

    private StatementTransactionResponse mapTransaction(
            Transaction txn) {

        return StatementTransactionResponse
                .builder()
                .transactionReference(
                        txn.getTransactionReference())
                .sourceAccount(
                        txn.getSourceAccount())
                .destinationAccount(
                        txn.getDestinationAccount())
                .transactionType(
                        txn.getTransactionType().name())
                .transactionStatus(
                        txn.getTransactionStatus().name())
                .amount(
                        txn.getAmount())
                .description(
                        txn.getDescription())
                .transactionDate(
                        txn.getTransactionDate())
                .build();
    }
}
