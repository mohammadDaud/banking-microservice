package com.bank.accs.client;

import com.bank.accs.config.FeignConfig;
import com.bank.accs.dtos.StatementTransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(
        name = "transaction-service",
        configuration = FeignConfig.class)
public interface TransactionClient {

    @GetMapping("/api/transactions/statement")
    public List<StatementTransactionResponse> getStatements(
            @RequestParam String accountNumber,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate);
}
