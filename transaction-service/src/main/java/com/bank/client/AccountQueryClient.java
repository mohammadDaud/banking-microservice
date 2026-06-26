package com.bank.client;

import com.bank.dtos.TransactionLimitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "account-service")
public interface AccountQueryClient {

    @GetMapping("/api/accounts/limits/{accountNumber}")
    TransactionLimitResponse getLimit(@PathVariable String accountNumber);
}