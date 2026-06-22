package com.bank.client;

import com.bank.dtos.KycEligibilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "kyc-service")
public interface KycClient {
    @GetMapping("/api/kyc/{userId}/eligibility")
    KycEligibilityResponse checkEligibility(@PathVariable String userId);
}
