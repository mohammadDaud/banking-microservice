package com.bank.client;

import com.bank.dtos.BeneficiaryEligibilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "beneficiary-service")
public interface BeneficiaryClient {

    @GetMapping("/api/beneficiaries/{beneficiaryId}/eligibility")
    BeneficiaryEligibilityResponse checkEligibility(
            @PathVariable String beneficiaryId,
            @RequestParam String customerId
    );
}