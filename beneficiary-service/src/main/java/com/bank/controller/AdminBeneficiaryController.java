package com.bank.controller;

import com.bank.dtos.BeneficiaryApprovalRequest;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/beneficiaries")
@RequiredArgsConstructor
public class AdminBeneficiaryController {

    private final BeneficiaryService service;

    @GetMapping("/pending")
    public List<BeneficiaryResponse> getPending() {
        return service.getPendingBeneficiaries();
    }

    @PutMapping("/{id}/approve")
    public BeneficiaryResponse approve(@PathVariable String id, @RequestBody BeneficiaryApprovalRequest request) {
        return service.approveBeneficiary(id, request);
    }

    @PutMapping("/{id}/reject")
    public BeneficiaryResponse reject(@PathVariable String id, @RequestBody BeneficiaryApprovalRequest request) {
        return service.rejectBeneficiary(id, request);
    }

    @GetMapping("/count")
    public Long count() {
        return service.count();
    }
}