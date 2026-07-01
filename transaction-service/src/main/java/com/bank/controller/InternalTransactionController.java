package com.bank.controller;


import com.bank.dtos.TransactionDeletionValidationResponse;
import com.bank.service.TransactionValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(
        name = "Internal Transaction APIs",
        description = "Internal APIs for inter-service communication"
)
public class InternalTransactionController {

    private final TransactionValidationService transactionValidationService;

    @Operation(summary = "Validate customer transactions before customer deletion")
    @GetMapping("/{customerId}/validation")
    public TransactionDeletionValidationResponse validateCustomerDeletion(@PathVariable String customerId) {
        return transactionValidationService.validateCustomerDeletion(customerId);
    }
}
