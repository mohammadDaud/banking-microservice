package com.bank.controller;

import com.bank.dtos.BeneficiaryEligibilityResponse;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;
import com.bank.service.BeneficiaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService service;

    @PostMapping
    public BeneficiaryResponse createBeneficiary(@RequestBody CreateBeneficiaryRequest request, @RequestHeader("X-User-Id") String makerId, HttpServletRequest httpServletRequest) {
        /*
         * Security:
         * customerId in body must match authenticated gateway user.
         */
        if (!makerId.equals(request.getCustomerId())) {
            throw new RuntimeException("You can create a beneficiary only for your own customer ID");
        }
        return service.createBeneficiary(request, makerId, httpServletRequest);
    }

    @GetMapping("/customer/{customerId}")
    public List<BeneficiaryResponse> getCustomerBeneficiaries(@PathVariable String customerId, @RequestHeader("X-User-Id") String loggedInUserId) {
        if (!loggedInUserId.equals(customerId)) {
            throw new RuntimeException("You can view only your own beneficiaries");
        }
        return service.getCustomerBeneficiaries(customerId);
    }

    /*
     * Internal API called by transaction-service.
     * Keep this accessible only through internal network later.
     */
    @GetMapping("/{beneficiaryId}/eligibility")
    public BeneficiaryEligibilityResponse checkEligibility(@PathVariable String beneficiaryId, @RequestParam String customerId) {
        return service.checkEligibility(beneficiaryId, customerId);
    }

    @DeleteMapping("/{beneficiaryId}")
    public void deleteBeneficiary(@PathVariable String beneficiaryId, @RequestHeader("X-User-Id") String loggedInUserId, HttpServletRequest httpServletRequest) {
        service.deleteBeneficiary(beneficiaryId, loggedInUserId, httpServletRequest);
    }

    @GetMapping("/customer/{customerId}/count")
    public Long getCount(@PathVariable String customerId, @RequestHeader("X-User-Id") String loggedInUserId) {
        if (!loggedInUserId.equals(customerId)) {
            throw new RuntimeException("You can view only your own beneficiary count");
        }
        return service.getBeneficiaryCount(customerId);
    }
}