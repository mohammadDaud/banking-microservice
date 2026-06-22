package com.bank.controller;

import com.bank.dtos.BeneficiaryEligibilityResponse;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;
import com.bank.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService service;

    @PostMapping
    public BeneficiaryResponse createBeneficiary(@RequestBody CreateBeneficiaryRequest request) {
        return service.createBeneficiary(request);
    }

    @GetMapping("/customer/{customerId}")
    public List<BeneficiaryResponse> getCustomerBeneficiaries(@PathVariable String customerId) {
        return service.getCustomerBeneficiaries(customerId);
    }

    @GetMapping("/{beneficiaryId}/eligibility")
    public BeneficiaryEligibilityResponse checkEligibility(@PathVariable String beneficiaryId,@RequestParam String customerId) {
        return service.checkEligibility(beneficiaryId, customerId);
    }

    @DeleteMapping("/{beneficiaryId}")
    public void deleteBeneficiary(@PathVariable String beneficiaryId) {
        service.deleteBeneficiary(beneficiaryId);
    }

    @GetMapping("/customer/{customerId}/count")
    public Long getCount(@PathVariable String customerId) {
        return service.getBeneficiaryCount(customerId);
    }


}