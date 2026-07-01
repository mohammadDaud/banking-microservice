package com.bank.us.client;

import com.bank.us.validation.CustomerDeletionValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "transaction-service"
)
public interface TransactionClient {

    @GetMapping("/api/transactions/{userId}/validation")
    CustomerDeletionValidationResponse validateCustomerDeletion(@PathVariable String userId);

}