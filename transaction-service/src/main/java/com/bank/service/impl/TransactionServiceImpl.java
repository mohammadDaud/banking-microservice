package com.bank.service.impl;

import com.bank.client.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final TransactionReferenceClient transactionReferenceClient;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final TransactionLimitValidator limitValidator;
    private final BeneficiaryClient beneficiaryClient;
    private final KycClient kycClient;
    private final RuleEngineClient ruleEngineClient;

    @Override
    @Transactional
    public TransactionResponse deposit(AmountTransactionRequest request) {

        AmountRequest amountRequest = new AmountRequest();
        amountRequest.setAmount(request.getAmount());

        accountClient.credit(request.getAccountNumber(), amountRequest);

        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getAccountNumber(),
                null,
                request.getAmount(),
                TransactionType.DEPOSIT,
                request.getDescription()
        );

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(request.getCustomerId())
                        .title("Deposit Successful")
                        .message("₹ " + request.getAmount() + " deposited successfully")
                        .type("TRANSACTION")
                        .priority("MEDIUM")
                        .build()
        );

        publishTransferAudit(
                transaction,
                "DEPOSIT_SUCCESS",
                "Deposit ₹" + transaction.getAmount() + " completed successfully",
                request.getCustomerId()
        );

        return map(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(AmountTransactionRequest request) {

        AmountRequest amountRequest = new AmountRequest();
        amountRequest.setAmount(request.getAmount());

        accountClient.debit(request.getAccountNumber(), amountRequest);

        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getAccountNumber(),
                null,
                request.getAmount(),
                TransactionType.WITHDRAW,
                request.getDescription()
        );

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(request.getCustomerId())
                        .title("Withdrawal Successful")
                        .message("₹ " + request.getAmount() + " withdrawn successfully")
                        .type("TRANSACTION")
                        .priority("MEDIUM")
                        .build()
        );

        publishTransferAudit(
                transaction,
                "WITHDRAW_SUCCESS",
                "Withdrawal ₹" + transaction.getAmount() + " completed successfully",
                request.getCustomerId()
        );

        return map(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        System.out.println("request================> :"+request.getBeneficiaryId());
        validateTransferRequest(request);

        /*
         * 1. Validate beneficiary using beneficiary ID.
         * Beneficiary service checks:
         * - beneficiary exists
         * - beneficiary belongs to this customer
         * - beneficiary status is APPROVED
         */
        BeneficiaryEligibilityResponse eligibility =
                beneficiaryClient.checkEligibility(
                        request.getBeneficiaryId(),
                        request.getCustomerId()
                );

        if (eligibility == null || !eligibility.isEligible()) {
            throw new RuntimeException(
                    "Transfer blocked: "
                            + (eligibility != null
                            ? eligibility.getMessage()
                            : "Beneficiary validation failed")
            );
        }

        /*
         * Destination account must come from beneficiary service,
         * never directly from Angular request.
         */
        String destinationAccount = eligibility.getAccountNumber();

        if (destinationAccount == null || destinationAccount.isBlank()) {
            throw new RuntimeException(
                    "Approved beneficiary account number is missing"
            );
        }

        if (request.getFromAccount().equals(destinationAccount)) {
            throw new RuntimeException(
                    "Cannot transfer to the same account"
            );
        }

        /*
         * 2. Customer KYC eligibility.
         */
        KycEligibilityResponse kycEligibility =
                kycClient.checkEligibility(request.getCustomerId());

        if (kycEligibility == null || !kycEligibility.isEligible()) {
            throw new RuntimeException(
                    "Transfer blocked: "
                            + (kycEligibility != null
                            ? kycEligibility.getMessage()
                            : "KYC validation failed")
            );
        }

        /*
         * 3. Validate daily/monthly transaction limits.
         */
        limitValidator.validateTransfer(
                request.getFromAccount(),
                request.getAmount()
        );

        /*
         * 4. Evaluate transfer through Rule Engine.
         */
        RuleEvaluationResponse ruleResult =
                evaluateTransferRule(request);

        if (ruleResult == null || ruleResult.getDecision() == null) {
            throw new RuntimeException(
                    "Rule engine did not return a valid transfer decision"
            );
        }

        /*
         * 5. Rule Engine rejects transfer.
         * No debit/credit happens.
         */
        if ("REJECT".equalsIgnoreCase(ruleResult.getDecision())) {

            Transaction rejectedTransaction = saveTransaction(
                    request.getCustomerId(),
                    request.getFromAccount(),
                    destinationAccount,
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    request.getDescription()
            );

            rejectedTransaction.setTransactionStatus(
                    TransactionStatus.REJECTED
            );
            rejectedTransaction.setRuleCode(
                    ruleResult.getMatchedRuleCode()
            );
            rejectedTransaction.setRuleReason(
                    ruleResult.getReason()
            );

            Transaction savedTransaction =
                    repository.save(rejectedTransaction);

            publishTransferAudit(
                    savedTransaction,
                    "TRANSFER_REJECTED_BY_RULE",
                    "Transfer rejected by BRE rule: "
                            + ruleResult.getReason(),
                    request.getCustomerId()
            );

            notificationClient.createNotification(
                    NotificationRequest.builder()
                            .userId(request.getCustomerId())
                            .title("Transfer Rejected")
                            .message(
                                    "Your transfer was rejected: "
                                            + ruleResult.getReason()
                            )
                            .type("TRANSACTION")
                            .priority("HIGH")
                            .build()
            );

            return map(savedTransaction);
        }

        /*
         * 6. Rule Engine requires maker-checker approval.
         * No debit/credit happens here.
         */
        if ("REQUIRE_CHECKER".equalsIgnoreCase(
                ruleResult.getDecision())) {

            Transaction pendingTransaction = saveTransaction(
                    request.getCustomerId(),
                    request.getFromAccount(),
                    destinationAccount,
                    request.getAmount(),
                    TransactionType.TRANSFER,
                    request.getDescription()
            );

            pendingTransaction.setTransactionStatus(
                    TransactionStatus.PENDING_APPROVAL
            );
            pendingTransaction.setRuleCode(
                    ruleResult.getMatchedRuleCode()
            );
            pendingTransaction.setRuleReason(
                    ruleResult.getReason()
            );

            Transaction savedTransaction =
                    repository.save(pendingTransaction);

            publishTransferAudit(
                    savedTransaction,
                    "TRANSFER_PENDING_APPROVAL",
                    "Transfer requires checker approval. Rule: "
                            + ruleResult.getMatchedRuleCode(),
                    request.getCustomerId()
            );

            notificationClient.createNotification(
                    NotificationRequest.builder()
                            .userId(request.getCustomerId())
                            .title("Transfer Pending Approval")
                            .message(
                                    "Your transfer of ₹"
                                            + request.getAmount()
                                            + " is pending checker approval"
                            )
                            .type("TRANSACTION")
                            .priority("HIGH")
                            .build()
            );

            kafkaEventPublisher.publish(
                    KafkaTopics.NOTIFICATION_TOPIC,
                    NotificationEvent.builder()
                            .userId(request.getCustomerId())
                            .title("Transfer Pending Approval")
                            .message(
                                    "₹" + request.getAmount()
                                            + " transfer is waiting for checker approval"
                            )
                            .type("TRANSACTION")
                            .priority("HIGH")
                            .build()
            );

            return map(savedTransaction);
        }

        /*
         * 7. Rule Engine approves transfer.
         * Debit source and credit verified beneficiary account.
         */
        AmountRequest debitRequest = new AmountRequest();
        debitRequest.setAmount(request.getAmount());

        accountClient.debit(
                request.getFromAccount(),
                debitRequest
        );

        AmountRequest creditRequest = new AmountRequest();
        creditRequest.setAmount(request.getAmount());

        accountClient.credit(
                destinationAccount,
                creditRequest
        );

        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getFromAccount(),
                destinationAccount,
                request.getAmount(),
                TransactionType.TRANSFER,
                request.getDescription()
        );

        transaction.setTransactionStatus(
                TransactionStatus.SUCCESS
        );
        transaction.setRuleCode(
                ruleResult.getMatchedRuleCode()
        );
        transaction.setRuleReason(
                ruleResult.getReason()
        );

        Transaction savedTransaction =
                repository.save(transaction);

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(request.getCustomerId())
                        .title("Transfer Successful")
                        .message(
                                "₹ " + request.getAmount()
                                        + " transferred successfully"
                        )
                        .type("TRANSACTION")
                        .priority("MEDIUM")
                        .build()
        );

        publishTransferAudit(
                savedTransaction,
                "TRANSFER_SUCCESS",
                "Transfer ₹" + savedTransaction.getAmount()
                        + " transferred successfully to beneficiary "
                        + eligibility.getBeneficiaryId()
                        + " (" + destinationAccount + ")",
                request.getCustomerId()
        );

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(savedTransaction.getCustomerId())
                        .title("Transaction Successful")
                        .message(
                                "₹" + savedTransaction.getAmount()
                                        + " transferred successfully"
                        )
                        .type("TRANSACTION")
                        .priority("MEDIUM")
                        .build()
        );

        return map(savedTransaction);
    }

    @Override
    public List<TransactionResponse> getCustomerTransactions(
            String customerId) {

        return repository
                .findByCustomerIdOrderByTransactionDateDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<TransactionResponse> getAccountTransactions(
            String accountNumber) {

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
    public List<TransactionResponse> getRecentTransactions(
            String customerId) {

        return repository
                .findTop5ByCustomerIdOrderByCreatedAtDesc(customerId)
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

        return repository.countTodayTransactions(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    @Override
    public List<Object[]> getMonthlyStats() {
        return repository.getMonthlyStats();
    }

    @Override
    public List<Transaction> findAllByOrderByTransactionDateDesc(
            PageRequest pageRequest) {

        return repository.findAllByOrderByTransactionDateDesc(
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize()
                )
        );
    }

    @Override
    public List<StatementTransactionResponse>
    findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
            String accountNumber,
            LocalDate fromDate,
            LocalDate toDate) {

        return repository
                .findBySourceAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
                        accountNumber,
                        fromDate.atStartOfDay(),
                        toDate.plusDays(1).atStartOfDay()
                )
                .stream()
                .map(this::mapTransaction)
                .toList();
    }

    @Override
    public List<TransactionResponse> getPendingApprovalTransactions() {

        return repository
                .findByTransactionStatusOrderByCreatedAtDesc(
                        TransactionStatus.PENDING_APPROVAL
                )
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional
    public TransactionResponse approvePendingTransaction(
            String transactionId,
            String checkerId,
            String remarks) {

        Transaction transaction =
                getPendingTransaction(transactionId);

        validateCheckerIsNotMaker(
                transaction,
                checkerId
        );

        AmountRequest debitRequest = new AmountRequest();
        debitRequest.setAmount(transaction.getAmount());

        accountClient.debit(
                transaction.getSourceAccount(),
                debitRequest
        );

        AmountRequest creditRequest = new AmountRequest();
        creditRequest.setAmount(transaction.getAmount());

        accountClient.credit(
                transaction.getDestinationAccount(),
                creditRequest
        );

        transaction.setTransactionStatus(
                TransactionStatus.SUCCESS
        );
        transaction.setCheckerId(checkerId);
        transaction.setCheckerRemarks(remarks);
        transaction.setCheckerActionAt(LocalDateTime.now());

        Transaction approvedTransaction =
                repository.save(transaction);

        publishTransferAudit(
                approvedTransaction,
                "TRANSFER_APPROVED_BY_CHECKER",
                "Transfer approved by checker: " + checkerId,
                checkerId
        );

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(approvedTransaction.getCustomerId())
                        .title("Transfer Approved")
                        .message(
                                "Your transfer of ₹"
                                        + approvedTransaction.getAmount()
                                        + " has been approved and completed"
                        )
                        .type("TRANSACTION")
                        .priority("HIGH")
                        .build()
        );

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(approvedTransaction.getCustomerId())
                        .title("Transfer Approved")
                        .message(
                                "₹" + approvedTransaction.getAmount()
                                        + " transfer has been approved"
                        )
                        .type("TRANSACTION")
                        .priority("HIGH")
                        .build()
        );

        return map(approvedTransaction);
    }

    @Override
    @Transactional
    public TransactionResponse rejectPendingTransaction(
            String transactionId,
            String checkerId,
            String remarks) {

        if (remarks == null || remarks.isBlank()) {
            throw new RuntimeException(
                    "Remarks are required when rejecting a transaction"
            );
        }

        Transaction transaction =
                getPendingTransaction(transactionId);

        validateCheckerIsNotMaker(
                transaction,
                checkerId
        );

        transaction.setTransactionStatus(
                TransactionStatus.REJECTED
        );
        transaction.setCheckerId(checkerId);
        transaction.setCheckerRemarks(remarks);
        transaction.setCheckerActionAt(LocalDateTime.now());

        Transaction rejectedTransaction =
                repository.save(transaction);

        publishTransferAudit(
                rejectedTransaction,
                "TRANSFER_REJECTED_BY_CHECKER",
                "Transfer rejected by checker: " + checkerId,
                checkerId
        );

        notificationClient.createNotification(
                NotificationRequest.builder()
                        .userId(rejectedTransaction.getCustomerId())
                        .title("Transfer Rejected")
                        .message(
                                "Your transfer of ₹"
                                        + rejectedTransaction.getAmount()
                                        + " was rejected by checker. Remarks: "
                                        + remarks
                        )
                        .type("TRANSACTION")
                        .priority("HIGH")
                        .build()
        );

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(rejectedTransaction.getCustomerId())
                        .title("Transfer Rejected")
                        .message(
                                "₹" + rejectedTransaction.getAmount()
                                        + " transfer was rejected"
                        )
                        .type("TRANSACTION")
                        .priority("HIGH")
                        .build()
        );

        return map(rejectedTransaction);
    }

    private void validateTransferRequest(TransferRequest request) {

        if (request.getCustomerId() == null
                || request.getCustomerId().isBlank()) {
            throw new RuntimeException("Customer ID is required");
        }

        if (request.getFromAccount() == null
                || request.getFromAccount().isBlank()) {
            throw new RuntimeException("Source account is required");
        }

        if (request.getBeneficiaryId() == null
                || request.getBeneficiaryId().isBlank()) {
            throw new RuntimeException("Beneficiary is required");
        }

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Transfer amount must be greater than zero"
            );
        }
    }

    private Transaction getPendingTransaction(
            String transactionId) {

        return repository.findByIdAndTransactionStatus(
                        transactionId,
                        TransactionStatus.PENDING_APPROVAL
                )
                .orElseThrow(() -> new RuntimeException(
                        "Pending transaction not found or already processed: "
                                + transactionId
                ));
    }

    private void validateCheckerIsNotMaker(
            Transaction transaction,
            String checkerId) {

        if (checkerId == null || checkerId.isBlank()) {
            throw new RuntimeException(
                    "Checker ID is required"
            );
        }

        if (transaction.getMakerId() != null
                && transaction.getMakerId()
                .equals(checkerId)) {
            throw new RuntimeException(
                    "Maker cannot approve or reject their own transaction"
            );
        }
    }

    private RuleEvaluationResponse evaluateTransferRule(
            TransferRequest request) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("amount", request.getAmount());

        /*
         * Temporary values.
         * Replace later with customer profile and beneficiary bank data.
         */
        payload.put("customerType", "REGULAR");
        payload.put("bankType", "EXTERNAL");

        return ruleEngineClient.evaluate(
                RuleEvaluationRequest.builder()
                        .ruleType("TRANSFER")
                        .payload(payload)
                        .build()
        );
    }

    private Transaction saveTransaction(
            String customerId,
            String sourceAccount,
            String destinationAccount,
            BigDecimal amount,
            TransactionType type,
            String description) {

        String referenceNumber =
                transactionReferenceClient
                        .generateTransactionReference()
                        .getReferenceNumber();

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .transactionReference(referenceNumber)
                .customerId(customerId)
                .makerId(customerId)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .transactionType(type)
                .transactionStatus(TransactionStatus.SUCCESS)
                .amount(amount)
                .description(description)
                .transactionDate(now)
                .createdAt(now)
                .build();

        return repository.save(transaction);
    }

    private TransactionResponse map(
            Transaction transaction) {

        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(
                        transaction.getTransactionReference()
                )
                .sourceAccount(
                        transaction.getSourceAccount()
                )
                .destinationAccount(
                        transaction.getDestinationAccount()
                )
                .transactionType(
                        transaction.getTransactionType()
                )
                .transactionStatus(
                        transaction.getTransactionStatus()
                )
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(
                        transaction.getTransactionDate()
                )
                .makerId(transaction.getMakerId())
                .checkerId(transaction.getCheckerId())
                .checkerRemarks(
                        transaction.getCheckerRemarks()
                )
                .checkerActionAt(
                        transaction.getCheckerActionAt()
                )
                .ruleCode(transaction.getRuleCode())
                .ruleReason(transaction.getRuleReason())
                .build();
    }

    private StatementTransactionResponse mapTransaction(
            Transaction transaction) {

        return StatementTransactionResponse.builder()
                .transactionReference(
                        transaction.getTransactionReference()
                )
                .sourceAccount(
                        transaction.getSourceAccount()
                )
                .destinationAccount(
                        transaction.getDestinationAccount()
                )
                .transactionType(
                        transaction.getTransactionType().name()
                )
                .transactionStatus(
                        transaction.getTransactionStatus().name()
                )
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(
                        transaction.getTransactionDate()
                )
                .build();
    }

    private void publishTransferAudit(
            Transaction transaction,
            String action,
            String description,
            String performedBy) {

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(performedBy)
                        .module("TRANSACTION")
                        .action(action)
                        .entityId(transaction.getId())
                        .entityType("TRANSACTION")
                        .ipAddress("127.0.0.1")
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}