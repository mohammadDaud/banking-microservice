package com.bank.controller;

import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CheckerActionRequest;
import com.bank.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/beneficiaries")
@RequiredArgsConstructor
public class AdminBeneficiaryController {

    private final BeneficiaryService service;

    @GetMapping("/pending")
    public List<BeneficiaryResponse> getPending(@RequestHeader("X-Roles") String roles) {
        validateCheckerRole(roles);
        return service.getPendingBeneficiaries();
    }

    @PutMapping("/{id}/approve")
    public BeneficiaryResponse approve(@PathVariable String id, @RequestHeader("X-User-Id") String checkerId, @RequestHeader("X-Roles") String roles, @RequestBody CheckerActionRequest request) {
        validateCheckerRole(roles);
        return service.approveBeneficiary(id, checkerId, request.getRemarks());
    }

    @PutMapping("/{id}/reject")
    public BeneficiaryResponse reject(@PathVariable String id, @RequestHeader("X-User-Id") String checkerId, @RequestHeader("X-Roles") String roles, @RequestBody CheckerActionRequest request) {
        validateCheckerRole(roles);
        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new RuntimeException("Remarks are required when rejecting a beneficiary");
        }
        return service.rejectBeneficiary(id, checkerId, request.getRemarks());
    }

    @GetMapping("/count")
    public Long count(@RequestHeader("X-Roles") String roles) {
        validateCheckerRole(roles);
        return service.count();
    }

    private void validateCheckerRole(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new RuntimeException("User roles are missing");
        }

        boolean allowed = Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role ->
                        "ROLE_ADMIN".equalsIgnoreCase(role)
                                || "ROLE_CHECKER".equalsIgnoreCase(role)
                );

        if (!allowed) {
            throw new RuntimeException("Only ADMIN or CHECKER can approve or reject beneficiaries");
        }
    }
}