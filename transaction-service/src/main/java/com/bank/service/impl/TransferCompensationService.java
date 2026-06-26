package com.bank.service.impl;

import com.bank.client.AccountMoneyClient;
import com.bank.common.events.AuditEvent;
import com.bank.common.events.NotificationEvent;
import com.bank.common.topics.KafkaTopics;
import com.bank.dtos.AmountRequest;
import com.bank.enums.TransactionStatus;
import com.bank.kafka.KafkaEventPublisher;
import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferCompensationService {

    private final AccountMoneyClient accountClient;
    private final TransactionRepository transactionRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateAfterCreditFailure(
            String transactionId,
            String originalFailureReason) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId
                ));

        transaction.setReversalAttemptedAt(LocalDateTime.now());
        transaction.setFailureReason(
                "Destination credit failed: " + safeMessage(originalFailureReason)
        );

        transactionRepository.saveAndFlush(transaction);

        try {
            AmountRequest reversalRequest = new AmountRequest();
            reversalRequest.setAmount(transaction.getAmount());

            /*
             * Reverse source account debit.
             */
            accountClient.credit(
                    transaction.getSourceAccount(),
                    reversalRequest
            );

            transaction.setTransactionStatus(TransactionStatus.FAILED);
            transaction.setReversalCompletedAt(LocalDateTime.now());
            transaction.setFailureReason(
                    "Destination credit failed. Source-account debit was reversed. "
                            + safeMessage(originalFailureReason)
            );

            transactionRepository.saveAndFlush(transaction);

            publishAudit(
                    transaction,
                    "TRANSFER_COMPENSATED",
                    "Destination credit failed; source account debit was reversed"
            );

            publishNotification(
                    transaction,
                    "Transfer Failed - Amount Reversed",
                    "Your transfer of ₹" + transaction.getAmount()
                            + " failed. The amount was returned to your account.",
                    "HIGH"
            );

        } catch (Exception reversalException) {

            log.error(
                    "Transfer reversal failed. transactionId={}",
                    transactionId,
                    reversalException
            );

            transaction.setTransactionStatus(TransactionStatus.REVERSAL_REQUIRED);
            transaction.setFailureReason(
                    "Destination credit failed and source-account reversal also failed. "
                            + "Credit error: " + safeMessage(originalFailureReason)
                            + " | Reversal error: "
                            + safeMessage(reversalException.getMessage())
            );

            transactionRepository.saveAndFlush(transaction);

            publishAudit(
                    transaction,
                    "TRANSFER_REVERSAL_REQUIRED",
                    "URGENT: destination credit and source reversal both failed"
            );

            publishNotification(
                    transaction,
                    "Transfer Requires Manual Review",
                    "Your transfer is under manual review. Please contact support.",
                    "HIGH"
            );
        }
    }

    private void publishAudit(
            Transaction transaction,
            String action,
            String description) {

        kafkaEventPublisher.publish(
                KafkaTopics.AUDIT_LOG_TOPIC,
                AuditEvent.builder()
                        .userId(transaction.getCustomerId())
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

    private void publishNotification(
            Transaction transaction,
            String title,
            String message,
            String priority) {

        kafkaEventPublisher.publish(
                KafkaTopics.NOTIFICATION_TOPIC,
                NotificationEvent.builder()
                        .userId(transaction.getCustomerId())
                        .title(title)
                        .message(message)
                        .type("TRANSACTION")
                        .priority(priority)
                        .build()
        );
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank()
                ? "Unknown error"
                : message;
    }
}