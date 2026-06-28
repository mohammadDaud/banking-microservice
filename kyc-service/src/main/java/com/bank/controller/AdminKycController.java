package com.bank.controller;

import com.bank.dtos.KycApprovalRequest;
import com.bank.dtos.KycDashboardResponse;
import com.bank.dtos.KycResponse;
import com.bank.dtos.KycStatResponse;
import com.bank.service.KycService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService service;

    @GetMapping("/pending")
    public List<KycResponse> getPendingKyc(
            @RequestHeader("X-Roles") String roles) {

        validateCheckerRole(roles);
        return service.getCheckerQueue();
    }

    @PutMapping("/{userId}/review")
    public KycResponse review(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String checkerId,
            @RequestHeader("X-Roles") String roles,
            @RequestBody KycApprovalRequest request,
            HttpServletRequest httpServletRequest) {

        validateCheckerRole(roles);
        return service.reviewKyc(userId, checkerId, request.getRemark(), httpServletRequest);
    }

    @GetMapping
    public List<KycResponse> getByStatus(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestHeader("X-Roles") String roles) {

        validateCheckerRole(roles);
        return service.getKycByStatus(status);
    }

    @PutMapping("/{userId}/approve")
    public KycResponse approve(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String checkerId,
            @RequestHeader("X-Roles") String roles,
            @RequestBody KycApprovalRequest request,
            HttpServletRequest httpServletRequest) {

        validateCheckerRole(roles);
        return service.approveKyc(userId, checkerId, request.getRemark(), httpServletRequest);
    }

    @PutMapping("/{userId}/reject")
    public KycResponse reject(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String checkerId,
            @RequestHeader("X-Roles") String roles,
            @RequestBody KycApprovalRequest request,
            HttpServletRequest httpServletRequest) {

        validateCheckerRole(roles);

        if (request.getRemark() == null || request.getRemark().isBlank()) {
            throw new IllegalArgumentException(
                    "Remark is required when rejecting KYC"
            );
        }

        return service.rejectKyc(userId, checkerId, request.getRemark(), httpServletRequest);
    }

    @GetMapping("/pending-count")
    public Long pendingCount(
            @RequestHeader("X-Roles") String roles) {

        validateCheckerRole(roles);
        return service.countByStatus("PENDING");
    }

    @GetMapping("/stats")
    public List<KycStatResponse> stats(
            @RequestHeader("X-Roles") String roles) {

        validateCheckerRole(roles);

        return service.getStats()
                .stream()
                .map(recod -> KycStatResponse.builder()
                        .status((String) recod[0])
                        .count(((Number) recod[1]).longValue())
                        .build())
                .toList();
    }

    @GetMapping("/dashboard/stats")
    public KycDashboardResponse dashboardStats() {
        return service.getDashboardStats();
    }

    private void validateCheckerRole(String roles) {
        if (roles == null || roles.isBlank()) {
            throw new IllegalArgumentException("User roles are missing");
        }

        boolean allowed = Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role ->
                        "ROLE_ADMIN".equalsIgnoreCase(role)
                                || "ROLE_CHECKER".equalsIgnoreCase(role)
                );

        if (!allowed) {
            throw new IllegalStateException(
                    "Only ADMIN or CHECKER can perform KYC checker actions"
            );
        }
    }
}