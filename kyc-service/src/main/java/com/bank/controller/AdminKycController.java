package com.bank.controller;

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

    @PutMapping("/{userId}/approve")
    public KycResponse approve(@PathVariable String userId) {
        return service.approveKyc(userId);
    }

    @PutMapping("/{userId}/reject")
    public KycResponse reject(@PathVariable String userId,
                              @RequestParam(required = false, defaultValue = "")
                              String remarks) {
        return service.rejectKyc(userId,remarks);
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
                        .count((Long) record[1])
                        .build())
                .toList();
    }
}