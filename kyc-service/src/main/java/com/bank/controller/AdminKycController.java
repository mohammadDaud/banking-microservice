package com.bank.controller;

import com.bank.dtos.KycApprovalRequest;
import com.bank.dtos.KycResponse;
import com.bank.dtos.KycStatResponse;
import com.bank.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private final KycService service;

    @GetMapping("/pending")
    public List<KycResponse> getPendingKyc() {
        return service.getPendingKyc();
    }

    @PutMapping("/{userId}/review")
    public KycResponse review(@PathVariable String userId,@RequestBody KycApprovalRequest request) {
        return service.reviewKyc(userId, request);
    }

    @GetMapping
    public List<KycResponse> getByStatus(@RequestParam(defaultValue = "PENDING") String status) {
        return service.getKycByStatus(status);
    }

    @PutMapping("/{userId}/approve")
    public KycResponse approve(@PathVariable String userId,@RequestBody KycApprovalRequest request) {
        return service.approveKyc(userId, request);
    }

    @PutMapping("/{userId}/reject")
    public KycResponse reject(@PathVariable String userId,@RequestBody KycApprovalRequest request) {
        return service.rejectKyc(userId, request);
    }

    @GetMapping("/pending-count")
    public Long pendingCount() {
        return service.countByStatus("PENDING");
    }

    @GetMapping("/stats")
    public List<KycStatResponse> stats() {
        return service.getStats()
                .stream()
                .map(record -> KycStatResponse.builder()
                        .status((String) record[0])
                        .count(((Number) record[1]).longValue())
                        .build())
                .toList();
    }
}