package com.bank.client;

import com.bank.dtos.TransactionReferenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "accountnumbergen-service")
public interface TransactionReferenceClient {
    @PostMapping("/api/sequences/transaction")
    TransactionReferenceResponse generateTransactionReference();
}
