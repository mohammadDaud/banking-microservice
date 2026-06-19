package com.bank.controller;

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

    @PutMapping("/{beneficiaryId}/approve")
    public BeneficiaryResponse approveBeneficiary(@PathVariable String beneficiaryId) {
        return service.approveBeneficiary(beneficiaryId);
    }

    @PutMapping("/{beneficiaryId}/reject")
    public BeneficiaryResponse rejectBeneficiary(@PathVariable String beneficiaryId) {
        return service.rejectBeneficiary(beneficiaryId);
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