package com.bank.service;


import com.bank.dtos.TransactionDeletionValidationResponse;
import com.bank.dtos.ValidationError;
import com.bank.enums.TransactionStatus;
import com.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidationService {

    private static final String MODULE = "TRANSACTION";

    private static final String SERVICE_NAME = "TRANSACTION-SERVICE";

    private final TransactionRepository repository;

    public TransactionDeletionValidationResponse validateCustomerDeletion(String customerId) {
        log.info("Starting transaction validation. customerId={}", customerId);
        List<ValidationError> errors = new ArrayList<>();
        validatePendingTransactions(customerId, errors);
        validatePendingApproval(customerId, errors);
        validateProcessingApproval(customerId, errors);
        //validateFailedTransactions(customerId, errors);
        validateReversalRequired(customerId, errors);
        validatePendingAmount(customerId, errors);
        TransactionDeletionValidationResponse response =
                TransactionDeletionValidationResponse
                        .builder()
                        .allowed(errors.isEmpty())
                        .serviceName(SERVICE_NAME)
                        .validatedAt(LocalDateTime.now())
                        .errors(errors)
                        .build();

        log.info("Transaction validation completed. customerId={}, allowed={}, errorCount={}",
                customerId,
                response.isAllowed(),
                response.getErrors().size()
        );

        return response;

    }

    private void validatePendingTransactions(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndTransactionStatus(customerId, TransactionStatus.PENDING)) {
            addError(
                    errors,
                    "PENDING_TRANSACTION",
                    "Customer has pending transaction(s)."
            );
        }
    }

    private void validatePendingApproval(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndTransactionStatus(customerId, TransactionStatus.PENDING_APPROVAL)) {
            addError(errors,
                    "PENDING_APPROVAL",
                    "Customer has pending approval transaction(s)."
            );
        }
    }

    private void validateProcessingApproval(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndTransactionStatus(customerId, TransactionStatus.PROCESSING_APPROVAL)) {
            addError(errors,
                    "PROCESSING_APPROVAL",
                    "Customer has transaction(s) under checker processing."
            );
        }
    }

//    private void validateFailedTransactions(String customerId, List<ValidationError> errors) {
//        if (repository.existsByCustomerIdAndTransactionStatus(customerId, TransactionStatus.FAILED)) {
//            addError(errors,
//                    "FAILED_TRANSACTION",
//                    "Customer has failed transaction(s). Verify before deletion."
//            );
//        }
//    }

    private void validateReversalRequired(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndTransactionStatus(customerId, TransactionStatus.REVERSAL_REQUIRED)) {
            addError(errors,
                    "REVERSAL_REQUIRED",
                    "Customer has transaction(s) waiting for reversal."
            );
        }
    }

    private void validatePendingAmount(String customerId, List<ValidationError> errors) {
        BigDecimal amount = repository.getPendingTransactionAmount(customerId);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            addError(errors,
                    "PENDING_AMOUNT",
                    "Pending transaction amount ₹" + amount
            );
        }
    }

    private void addError(List<ValidationError> errors, String code, String message) {
        errors.add(ValidationError.builder()
                .module(MODULE)
                .code(code)
                .message(message)
                .build()
        );
    }

}
