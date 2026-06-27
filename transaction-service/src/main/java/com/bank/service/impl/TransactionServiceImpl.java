package com.bank.service.impl;

import com.bank.client.*;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.*;
import com.bank.enums.TransactionStatus;
import com.bank.enums.TransactionType;
import com.bank.exception.RuleEngineUnavailableException;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import com.bank.security.InternalServiceTokenProvider;
import com.bank.service.TransactionService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final AccountMoneyClient accountClient;
    private final TransactionReferenceClient transactionReferenceClient;
    private final NotificationClient notificationClient;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final TransactionLimitValidator limitValidator;
    private final BeneficiaryClient beneficiaryClient;
    private final KycClient kycClient;
    private final RuleEngineClient ruleEngineClient;

    private final TransactionApprovalRecoveryService approvalRecoveryService;
    private final TransferCompensationService transferCompensationService;
    private final TransferExecutionService transferExecutionService;
    private final InternalServiceTokenProvider tokenProvider;

    @Override
    @Transactional
    public TransactionResponse deposit(AmountTransactionRequest request) {
        validateAmountTransactionRequest(request, "Deposit");
        creditAccount(request.getAccountNumber(), request.getAmount());
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
        validateAmountTransactionRequest(request, "Withdrawal");
        debitAccount(request.getAccountNumber(), request.getAmount());
        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getAccountNumber(),
                null,
                request.getAmount(),
                TransactionType.WITHDRAW,
                request.getDescription()
        );

        publishNotification(
                request.getCustomerId(),
                "Withdrawal Successful",
                "₹ " + request.getAmount() + " withdrawn successfully",
                "MEDIUM"
        );

        publishTransferAudit(
                transaction,
                "WITHDRAW_SUCCESS",
                "Withdrawal ₹" + transaction.getAmount() + " completed successfully",
                request.getCustomerId()
        );

        return map(transaction);
    }

    /*
     * No @Transactional here.
     *
     * This method calls remote Account Service APIs.
     * Transaction database status changes are saved through
     * TransferExecutionService using REQUIRES_NEW transactions.
     */
    @Override
    public TransactionResponse transfer(TransferRequest request) {
        log.info(
                "Transfer request received. customerId={}, beneficiaryId={}, amount={}",
                request.getCustomerId(),
                request.getBeneficiaryId(),
                request.getAmount()
        );
        validateTransferRequest(request);
        BeneficiaryEligibilityResponse beneficiaryEligibility =
                beneficiaryClient.checkEligibility(
                        request.getBeneficiaryId(),
                        request.getCustomerId()
                );

        if (beneficiaryEligibility == null || !beneficiaryEligibility.isEligible()) {
            throw new RuntimeException(
                    "Transfer blocked: "
                            + (beneficiaryEligibility != null
                            ? beneficiaryEligibility.getMessage()
                            : "Beneficiary validation failed")
            );
        }

        String destinationAccount = beneficiaryEligibility.getAccountNumber();

        if (destinationAccount == null || destinationAccount.isBlank()) {
            throw new RuntimeException("Approved beneficiary account number is missing");
        }

        if (request.getFromAccount().equals(destinationAccount)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

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

        limitValidator.validateTransfer(
                request.getFromAccount(),
                request.getAmount()
        );

        RuleEvaluationResponse ruleResult = evaluateTransferRule(request);

        validateRuleDecision(ruleResult);

        if ("REJECT".equalsIgnoreCase(ruleResult.getDecision())) {
            return createRuleRejectedTransaction(
                    request,
                    destinationAccount,
                    ruleResult
            );
        }

        if ("REQUIRE_CHECKER".equalsIgnoreCase(ruleResult.getDecision())) {
            return createPendingApprovalTransaction(
                    request,
                    destinationAccount,
                    ruleResult
            );
        }

        return executeAutoApprovedTransfer(
                request,
                destinationAccount,
                ruleResult,
                beneficiaryEligibility.getBeneficiaryId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getCustomerTransactions(String customerId) {
        return repository.findByCustomerIdOrderByTransactionDateDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        return repository.findBySourceAccountOrderByTransactionDateDesc(accountNumber)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTransactionCount(String customerId) {
        return repository.countByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRecentTransactions(String customerId) {
        return repository.findTop5ByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTodayTransactions() {
        LocalDate today = LocalDate.now();
        return repository.countTodayTransactions(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyStats() {
        return repository.getMonthlyStats();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> findAllByOrderByTransactionDateDesc(PageRequest pageRequest) {
        return repository.findAllByOrderByTransactionDateDesc(
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPendingApprovalTransactions() {
        return repository
                .findByTransactionStatusOrderByCreatedAtDesc(
                        TransactionStatus.PENDING_APPROVAL
                )
                .stream()
                .map(this::map)
                .toList();
    }

    /*
     * No @Transactional here.
     *
     * The claim query, success update, recovery update and compensation
     * update all use separate REQUIRES_NEW transactions.
     */
    @Override
    public TransactionResponse approvePendingTransaction(String transactionId, String checkerId, String remarks) {
        validateCheckerId(checkerId);
        Transaction pendingTransaction = repository
                .findByIdAndTransactionStatus(transactionId, TransactionStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new RuntimeException("Pending transaction not found or already processed: " + transactionId
                ));

        validateCheckerIsNotMaker(pendingTransaction, checkerId);
        int claimed = repository.claimPendingTransactionForApproval(
                transactionId,
                TransactionStatus.PENDING_APPROVAL,
                TransactionStatus.PROCESSING_APPROVAL,
                checkerId,
                remarks,
                LocalDateTime.now()
        );

        if (claimed == 0) {
            throw new RuntimeException("This transaction was already claimed or processed by another checker");
        }

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found after approval claim: " + transactionId));

        boolean sourceDebited = false;

        try {
            debitAccount(transaction.getSourceAccount(), transaction.getAmount());
            sourceDebited = true;
            creditAccount(transaction.getDestinationAccount(), transaction.getAmount());
            Transaction approvedTransaction =
                    transferExecutionService.markSuccess(transaction.getId());
            publishTransferAudit(
                    approvedTransaction,
                    "TRANSFER_APPROVED_BY_CHECKER",
                    "Transfer approved by checker: " + checkerId,
                    checkerId
            );
            publishNotification(
                    approvedTransaction.getCustomerId(),
                    "Transfer Approved",
                    "₹" + approvedTransaction.getAmount()
                            + " transfer has been approved",
                    "HIGH"
            );
            return map(approvedTransaction);

        } catch (Exception ex) {
            log.error(
                    "Checker approval transfer failed. transactionId={}",
                    transactionId,
                    ex
            );
            if (sourceDebited) {
                transferCompensationService.compensateAfterCreditFailure(
                        transactionId,
                        safeExceptionMessage(ex)
                );
            } else {
                approvalRecoveryService.resetToPending(transactionId);
            }

            throw new RuntimeException(
                    sourceDebited
                            ? "Transfer approval failed. Reversal was started for the debited amount."
                            : "Transfer approval failed before debit. Transaction moved back to pending approval.",
                    ex
            );
        }
    }

    @Override
    @Transactional
    public TransactionResponse rejectPendingTransaction(
            String transactionId,
            String checkerId,
            String remarks) {

        validateCheckerId(checkerId);

        if (remarks == null || remarks.isBlank()) {
            throw new RuntimeException(
                    "Remarks are required when rejecting a transaction"
            );
        }

        Transaction transaction = getPendingTransaction(transactionId);
        validateCheckerIsNotMaker(transaction, checkerId);
        transaction.setTransactionStatus(TransactionStatus.REJECTED);
        transaction.setCheckerId(checkerId);
        transaction.setCheckerRemarks(remarks);
        transaction.setCheckerActionAt(LocalDateTime.now());

        try {
            Transaction rejectedTransaction = repository.saveAndFlush(transaction);
            publishTransferAudit(
                    rejectedTransaction,
                    "TRANSFER_REJECTED_BY_CHECKER",
                    "Transfer rejected by checker: " + checkerId,
                    checkerId
            );
            publishNotification(
                    rejectedTransaction.getCustomerId(),
                    "Transfer Rejected",
                    "Your transfer of ₹" + rejectedTransaction.getAmount()
                            + " was rejected by checker. Remarks: " + remarks,
                    "HIGH"
            );
            return map(rejectedTransaction);

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw new RuntimeException(
                    "This transaction was already processed by another checker. Refresh the list."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getReversalRequiredTransactions() {
        return repository
                .findByTransactionStatusOrderByCreatedAtDesc(TransactionStatus.REVERSAL_REQUIRED)
                .stream()
                .map(this::map)
                .toList();
    }

    private TransactionResponse createRuleRejectedTransaction(
            TransferRequest request,
            String destinationAccount,
            RuleEvaluationResponse ruleResult) {

        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getFromAccount(),
                destinationAccount,
                request.getAmount(),
                TransactionType.TRANSFER,
                request.getDescription()
        );

        transaction.setTransactionStatus(TransactionStatus.REJECTED);
        transaction.setRuleCode(ruleResult.getMatchedRuleCode());
        transaction.setRuleReason(ruleResult.getReason());
        Transaction savedTransaction = repository.save(transaction);
        publishTransferAudit(
                savedTransaction,
                "TRANSFER_REJECTED_BY_RULE",
                "Transfer rejected by BRE rule: " + safeReason(ruleResult),
                request.getCustomerId()
        );

        publishNotification(
                request.getCustomerId(),
                "Transfer Rejected",
                "Your transfer was rejected: " + safeReason(ruleResult),
                "HIGH"
        );

        return map(savedTransaction);
    }

    private TransactionResponse createPendingApprovalTransaction(
            TransferRequest request,
            String destinationAccount,
            RuleEvaluationResponse ruleResult) {

        Transaction transaction = saveTransaction(
                request.getCustomerId(),
                request.getFromAccount(),
                destinationAccount,
                request.getAmount(),
                TransactionType.TRANSFER,
                request.getDescription()
        );

        transaction.setTransactionStatus(TransactionStatus.PENDING_APPROVAL);
        transaction.setRuleCode(ruleResult.getMatchedRuleCode());
        transaction.setRuleReason(ruleResult.getReason());
        Transaction savedTransaction = repository.save(transaction);
        publishTransferAudit(
                savedTransaction,
                "TRANSFER_PENDING_APPROVAL",
                "Transfer requires checker approval. Rule: "
                        + ruleResult.getMatchedRuleCode(),
                request.getCustomerId()
        );

        publishNotification(
                request.getCustomerId(),
                "Transfer Pending Approval",
                "₹" + request.getAmount()
                        + " transfer is waiting for checker approval",
                "HIGH"
        );

        return map(savedTransaction);
    }

    private TransactionResponse executeAutoApprovedTransfer(
            TransferRequest request,
            String destinationAccount,
            RuleEvaluationResponse ruleResult,
            String beneficiaryId) {

        Transaction transaction = buildTransaction(
                request.getCustomerId(),
                request.getFromAccount(),
                destinationAccount,
                request.getAmount(),
                TransactionType.TRANSFER,
                request.getDescription()
        );

        transaction.setRuleCode(ruleResult.getMatchedRuleCode());
        transaction.setRuleReason(ruleResult.getReason());
        Transaction processingTransaction =
                transferExecutionService.saveProcessing(transaction);
        boolean sourceDebited = false;
        try {
            debitAccount(
                    processingTransaction.getSourceAccount(),
                    processingTransaction.getAmount()
            );
            sourceDebited = true;
            creditAccount(
                    processingTransaction.getDestinationAccount(),
                    processingTransaction.getAmount()
            );
            Transaction savedTransaction =
                    transferExecutionService.markSuccess(processingTransaction.getId());
            publishTransferAudit(
                    savedTransaction,
                    "TRANSFER_SUCCESS",
                    "Transfer ₹" + savedTransaction.getAmount()
                            + " transferred successfully to beneficiary "
                            + beneficiaryId,
                    request.getCustomerId()
            );
            publishNotification(
                    savedTransaction.getCustomerId(),
                    "Transaction Successful",
                    "₹" + savedTransaction.getAmount()
                            + " transferred successfully",
                    "MEDIUM"
            );
            return map(savedTransaction);

        } catch (Exception ex) {
            log.error(
                    "Auto-approved transfer failed. transactionId={}",
                    processingTransaction.getId(),
                    ex
            );
            if (sourceDebited) {
                transferCompensationService.compensateAfterCreditFailure(
                        processingTransaction.getId(),
                        safeExceptionMessage(ex)
                );
            } else {
                transferExecutionService.markDebitFailed(
                        processingTransaction.getId(),
                        safeExceptionMessage(ex)
                );
            }
            throw new RuntimeException(
                    sourceDebited
                            ? "Transfer failed. Reversal was started for the debited amount."
                            : "Transfer failed. No amount was debited.",
                    ex
            );
        }
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request == null) {
            throw new RuntimeException("Transfer request is required");
        }

        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new RuntimeException("Customer ID is required");
        }

        if (request.getFromAccount() == null || request.getFromAccount().isBlank()) {
            throw new RuntimeException("Source account is required");
        }

        if (request.getBeneficiaryId() == null || request.getBeneficiaryId().isBlank()) {
            throw new RuntimeException("Beneficiary is required");
        }

        if (request.getAmount() == null
                || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }
    }

    private void validateAmountTransactionRequest(AmountTransactionRequest request, String transactionName) {
        if (request == null) {
            throw new RuntimeException(transactionName + " request is required");
        }

        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new RuntimeException("Customer ID is required");
        }

        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            throw new RuntimeException("Account number is required");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(transactionName + " amount must be greater than zero");
        }
    }

    private void validateRuleDecision(RuleEvaluationResponse ruleResult) {

        if (ruleResult == null
                || ruleResult.getDecision() == null
                || ruleResult.getDecision().isBlank()) {
            throw new RuntimeException("Rule Engine did not return a valid transfer decision");
        }
        String decision = ruleResult.getDecision().trim();
        boolean validDecision =
                "REJECT".equalsIgnoreCase(decision)
                        || "REQUIRE_CHECKER".equalsIgnoreCase(decision)
                        || "AUTO_APPROVE".equalsIgnoreCase(decision)
                        || "APPROVE".equalsIgnoreCase(decision);

        if (!validDecision) {
            throw new RuntimeException("Invalid Rule Engine decision: " + ruleResult.getDecision());
        }
    }

    private Transaction getPendingTransaction(String transactionId) {
        return repository.findByIdAndTransactionStatus(transactionId, TransactionStatus.PENDING_APPROVAL)
                .orElseThrow(() -> new RuntimeException("Pending transaction not found or already processed: " + transactionId
                ));
    }

    private void validateCheckerId(String checkerId) {
        if (checkerId == null || checkerId.isBlank()) {
            throw new RuntimeException("Checker ID is required");
        }
    }

    private void validateCheckerIsNotMaker(Transaction transaction, String checkerId) {
        if (transaction.getMakerId() != null && transaction.getMakerId().equals(checkerId)) {
            throw new RuntimeException("Maker cannot approve or reject their own transaction");
        }
    }

    private RuleEvaluationResponse evaluateTransferRule(TransferRequest request) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getAmount());

        /*
         * Replace later with real customer / beneficiary data.
         */
        payload.put("customerType", "REGULAR");
        payload.put("bankType", "EXTERNAL");

        try {
            RuleEvaluationResponse response = ruleEngineClient.evaluate(
                    RuleEvaluationRequest.builder()
                            .ruleType("TRANSFER")
                            .payload(payload)
                            .build()
            );

            if (response == null || response.getDecision() == null || response.getDecision().isBlank()) {
                throw new RuleEngineUnavailableException("Transfer cannot be processed because Rule " +
                        "Engine returned an invalid decision.", null);
            }

            return response;

        } catch (FeignException.ServiceUnavailable ex) {
            throw new RuleEngineUnavailableException("Transfer cannot be processed because Rule Engine Service is " +
                    "currently unavailable. Please try again later.", ex);

        } catch (FeignException ex) {
            throw new RuleEngineUnavailableException("Transfer cannot be processed because Rule Engine Service could not" +
                    " evaluate this transfer. Please try again later.", ex);

        } catch (RuleEngineUnavailableException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new RuleEngineUnavailableException("Transfer cannot be processed because Rule Engine Service is unavailable." +
                    " Please try again later.", ex);
        }
    }

    private void debitAccount(String accountNumber, BigDecimal amount) {
        AmountRequest request = new AmountRequest();
        request.setAmount(amount);
        String token = tokenProvider.getAccessToken();
        accountClient.debit(
                "Bearer " + token,
                accountNumber,
                request
        );
    }

    private void creditAccount(String accountNumber, BigDecimal amount) {
        AmountRequest request = new AmountRequest();
        request.setAmount(amount);
        String token = tokenProvider.getAccessToken();
        accountClient.credit("Bearer " + token,
                accountNumber,
                request
        );
    }

    private Transaction saveTransaction(
            String customerId,
            String sourceAccount,
            String destinationAccount,
            BigDecimal amount,
            TransactionType type,
            String description) {

        return repository.save(
                buildTransaction(
                        customerId,
                        sourceAccount,
                        destinationAccount,
                        amount,
                        type,
                        description
                )
        );
    }

    private Transaction buildTransaction(
            String customerId,
            String sourceAccount,
            String destinationAccount,
            BigDecimal amount,
            TransactionType type,
            String description) {

        String referenceNumber = transactionReferenceClient
                .generateTransactionReference()
                .getReferenceNumber();
        LocalDateTime now = LocalDateTime.now();
        return Transaction.builder()
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
    }

    private TransactionResponse map(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .sourceAccount(transaction.getSourceAccount())
                .destinationAccount(transaction.getDestinationAccount())
                .transactionType(transaction.getTransactionType())
                .transactionStatus(transaction.getTransactionStatus())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .makerId(transaction.getMakerId())
                .checkerId(transaction.getCheckerId())
                .checkerRemarks(transaction.getCheckerRemarks())
                .checkerActionAt(transaction.getCheckerActionAt())
                .ruleCode(transaction.getRuleCode())
                .ruleReason(transaction.getRuleReason())
                .build();
    }

    private StatementTransactionResponse mapTransaction(Transaction transaction) {
        return StatementTransactionResponse.builder()
                .transactionReference(transaction.getTransactionReference())
                .sourceAccount(transaction.getSourceAccount())
                .destinationAccount(transaction.getDestinationAccount())
                .transactionType(transaction.getTransactionType().name())
                .transactionStatus(transaction.getTransactionStatus().name())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }

    private void publishNotification(
            String userId,
            String title,
            String message,
            String priority) {

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(userId)
                        .title(title)
                        .message(message)
                        .type("TRANSACTION")
                        .priority(priority)
                        .build()
        );
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

    private String safeReason(RuleEvaluationResponse ruleResult) {
        return ruleResult.getReason() == null || ruleResult.getReason().isBlank()
                ? "Rule Engine rejected this transfer"
                : ruleResult.getReason();
    }

    private String safeExceptionMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}