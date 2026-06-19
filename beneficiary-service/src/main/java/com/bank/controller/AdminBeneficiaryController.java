package com.bank.controller;

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
    public BeneficiaryResponse approve(@PathVariable String id) {
        return service.approveBeneficiary(id);
    }

    @PutMapping("/{id}/reject")
    public BeneficiaryResponse reject(@PathVariable String id) {
        return service.rejectBeneficiary(id);
    }

    @GetMapping("/count")
    public Long count() {
        return service.count();
    }
}