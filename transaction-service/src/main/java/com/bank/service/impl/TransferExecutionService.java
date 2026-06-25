package com.bank.service.impl;

import com.bank.enums.TransactionStatus;
import com.bank.model.Transaction;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferExecutionService {

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction saveProcessing(Transaction transaction) {
        transaction.setTransactionStatus(TransactionStatus.PROCESSING_APPROVAL);
        return transactionRepository.saveAndFlush(transaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction markSuccess(String transactionId) {
        Transaction transaction = getTransaction(transactionId);
        transaction.setTransactionStatus(TransactionStatus.SUCCESS);
        transaction.setFailureReason(null);
        return transactionRepository.saveAndFlush(transaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction markDebitFailed(String transactionId, String failureReason) {
        Transaction transaction = getTransaction(transactionId);
        transaction.setTransactionStatus(TransactionStatus.FAILED);
        transaction.setFailureReason("Source-account debit failed: " + safeMessage(failureReason));
        return transactionRepository.saveAndFlush(transaction);
    }

    private Transaction getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId
                ));
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank()
                ? "Unknown error"
                : message;
    }
}