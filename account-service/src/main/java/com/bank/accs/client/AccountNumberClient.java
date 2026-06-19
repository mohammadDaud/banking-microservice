package com.bank.accs.client;

import com.bank.accs.config.FeignConfig;
import com.bank.accs.dtos.AccountNumberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "accountnumbergen-service",
        configuration = FeignConfig.class)
public interface AccountNumberClient {

    @PostMapping("/api/sequences/account")
    AccountNumberResponse
    generateAccountNumber();
}
