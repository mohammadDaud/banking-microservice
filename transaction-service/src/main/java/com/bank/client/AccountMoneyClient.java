package com.bank.client;

import com.bank.config.AccountFeignConfiguration;
import com.bank.dtos.AmountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "account-service",
        contextId = "accountMoneyClient",
        configuration = AccountFeignConfiguration.class
)
public interface AccountMoneyClient {

    @PutMapping("/api/accounts/{accountNumber}/credit")
    void credit(
            @RequestHeader("Authorization")
            String authorization,

            @PathVariable
            String accountNumber,

            @RequestBody
            AmountRequest request
    );

    @PutMapping("/api/accounts/{accountNumber}/debit")
    void debit(
            @RequestHeader("Authorization")
            String authorization,

            @PathVariable
            String accountNumber,

            @RequestBody
            AmountRequest request
    );

}