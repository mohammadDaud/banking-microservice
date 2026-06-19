package com.bank.accs.controller;

import com.bank.accs.dtos.TransactionLimitRequest;
import com.bank.accs.dtos.TransactionLimitResponse;
import com.bank.accs.service.TransactionLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class TransactionLimitController {

    private final TransactionLimitService service;

    @GetMapping("/limits/{accountNumber}")
    public TransactionLimitResponse getLimit(@PathVariable String accountNumber) {
        return service.getLimit(accountNumber);
    }

    @PutMapping("/{accountNumber}")
    public TransactionLimitResponse update(@PathVariable String accountNumber,@RequestBody TransactionLimitRequest request) {
        return service.updateLimit(accountNumber,request);
    }
}