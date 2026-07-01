package com.bank.accs.service;

import com.bank.accs.dtos.AccountDeletionValidationResponse;
import com.bank.accs.dtos.ValidationError;
import com.bank.accs.model.enums.AccountStatus;
import com.bank.accs.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidationService {

    private static final String MODULE = "ACCOUNT";

    private static final String SERVICE_NAME = "ACCOUNT-SERVICE";

    private final AccountRepository repository;

    public AccountDeletionValidationResponse validateCustomerDeletion(String customerId) {
        List<ValidationError> errors = new ArrayList<>();
        validateActiveAccounts(customerId, errors);
        validateFrozenAccounts(customerId, errors);
        validateAccountBalance(customerId, errors);
        validateOpenAccounts(customerId, errors);
        validateBlockedAccounts(customerId, errors);
        validateDormantAccounts(customerId, errors);
        validateLedgerBalance(customerId, errors);
        log.info("Validation completed. customerId={}, allowed={}, errors={}",
                customerId,
                errors.isEmpty(),
                errors.size()
        );
        return AccountDeletionValidationResponse.builder()
                .allowed(errors.isEmpty())
                .serviceName(SERVICE_NAME)
                .validatedAt(LocalDateTime.now())
                .errors(errors)
                .build();

    }

    public ResponseEntity<List<String>> getCustomerAccountNumbers(String customerId) {
        List<String> accountNumbers = repository.findAccountNumbersByCustomerId(customerId);
        return ResponseEntity.ok(accountNumbers);
    }

    private void validateActiveAccounts(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndAccountStatus(customerId, AccountStatus.ACTIVE)) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("ACTIVE_ACCOUNT")
                    .message("Customer has active account(s).")
                    .build()
            );
        }
    }

    private void validateFrozenAccounts(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndAccountStatus(customerId, AccountStatus.FROZEN)) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("FROZEN_ACCOUNT")
                    .message("Customer has frozen account(s).")
                    .build()
            );
        }
    }

    private void validateAccountBalance(String customerId, List<ValidationError> errors) {
        BigDecimal balance = repository.getCustomerTotalBalance(customerId);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("NON_ZERO_BALANCE")
                    .message("Customer balance must be zero before deletion.")
                    .build()
            );
        }
    }

    private void validateOpenAccounts(String customerId, List<ValidationError> errors) {
        if (repository.countOpenAccounts(customerId) > 0) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("OPEN_ACCOUNT")
                    .message("Close all customer accounts before deleting the customer.")
                    .build()
            );
        }
    }

    private void validateBlockedAccounts(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndAccountStatus(customerId, AccountStatus.BLOCKED)) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("BLOCKED_ACCOUNT")
                    .message("Customer has blocked account(s).")
                    .build()

            );
        }
    }

    private void validateDormantAccounts(String customerId, List<ValidationError> errors) {
        if (repository.existsByCustomerIdAndAccountStatus(customerId, AccountStatus.DORMANT)) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("DORMANT_ACCOUNT")
                    .message("Customer has dormant account(s).")
                    .build()
            );
        }
    }

    private void validateLedgerBalance(String customerId, List<ValidationError> errors) {
        BigDecimal ledgerBalance = repository.getCustomerLedgerBalance(customerId);
        if (ledgerBalance.compareTo(BigDecimal.ZERO) > 0) {
            errors.add(ValidationError.builder()
                    .module(MODULE)
                    .code("LEDGER_BALANCE")
                    .message("Ledger balance must be zero.")
                    .build()

            );
        }
    }


}