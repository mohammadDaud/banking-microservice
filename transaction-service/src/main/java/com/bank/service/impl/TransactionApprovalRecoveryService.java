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
public class TransactionApprovalRecoveryService {

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetToPending(String transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId
                ));

        transaction.setTransactionStatus(TransactionStatus.PENDING_APPROVAL);
        transaction.setCheckerId(null);
        transaction.setCheckerRemarks(null);
        transaction.setCheckerActionAt(null);

        transactionRepository.saveAndFlush(transaction);
    }
}