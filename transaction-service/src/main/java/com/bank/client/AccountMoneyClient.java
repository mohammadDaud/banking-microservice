package com.bank.client;

import com.bank.config.InternalFeignConfig;
import com.bank.dtos.AmountRequest;
import com.bank.dtos.TransactionLimitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "account-service",
        configuration = InternalFeignConfig.class
)
public interface AccountClient {

    @PutMapping("/api/accounts/{accountNumber}/credit")
    void credit(@PathVariable String accountNumber,@RequestBody AmountRequest request);

    @PutMapping("/api/accounts/{accountNumber}/debit")
    void debit(@PathVariable String accountNumber,@RequestBody AmountRequest request);

    @GetMapping("/api/accounts/limits/{accountNumber}")
    TransactionLimitResponse getLimit(@PathVariable String accountNumber);
}