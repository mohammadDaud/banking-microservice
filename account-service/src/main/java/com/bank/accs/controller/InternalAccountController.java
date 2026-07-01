package com.bank.accs.controller;

import com.bank.accs.dtos.AccountDeletionValidationResponse;
import com.bank.accs.service.AccountValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountValidationService service;

    @GetMapping("/{customerId}/validation")
    public AccountDeletionValidationResponse validateCustomerDeletion(@PathVariable String customerId) {
        return service.validateCustomerDeletion(customerId);
    }

    @GetMapping("/{customerId}/accountNumber")
    public ResponseEntity<List<String>> getCustomerAccountNumber(@PathVariable String customerId) {
        return service.getCustomerAccountNumbers(customerId);
    }
}
