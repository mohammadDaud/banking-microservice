package com.bank.acnumgens.controller;

import com.bank.acnumgens.dtos.AccountNumberResponse;
import com.bank.acnumgens.dtos.CustomerIdResponse;
import com.bank.acnumgens.dtos.TransactionReferenceResponse;
import com.bank.acnumgens.service.SequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sequences")
@RequiredArgsConstructor
public class SequenceController {

    private final SequenceService service;

    @PostMapping("/account")
    public AccountNumberResponse generateAccountNumber() {
        return AccountNumberResponse
                .builder()
                .accountNumber(
                        service.generateAccountNumber())
                .build();
    }

    @PostMapping("/customer")
    public CustomerIdResponse generateCustomerId() {
        return CustomerIdResponse
                .builder()
                .customerId(
                        service.generateCustomerId())
                .build();
    }

    @PostMapping("/transaction")
    public TransactionReferenceResponse generateTransactionReference() {
        return TransactionReferenceResponse
                .builder()
                .referenceNumber(
                        service.generateTransactionReference())
                .build();
    }
}