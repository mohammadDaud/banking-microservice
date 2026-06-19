package com.bank.controller;

import com.bank.dtos.CreateKycRequest;
import com.bank.dtos.KycResponse;
import com.bank.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService service;

    @PostMapping(consumes =MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycResponse createKyc(
            @RequestParam String userId,
            @RequestParam String panNumber,
            @RequestParam String aadhaarNumber,
            @RequestPart MultipartFile panDocument,
            @RequestPart MultipartFile aadhaarDocument) {
        return service.createKyc(userId,panNumber,aadhaarNumber,panDocument,aadhaarDocument);
    }
    @GetMapping("/{userId}")
    public KycResponse getKyc(@PathVariable String userId) {
        return service.getKyc(userId);
    }

    @PutMapping("/{userId}/approve")
    public KycResponse approve(@PathVariable String userId) {
        return service.approveKyc(userId);
    }

    @PutMapping("/{userId}/reject")
    public KycResponse reject(@PathVariable String userId,@RequestParam String remarks) {
        return service.rejectKyc(userId,remarks);
    }

    @PutMapping("/{userId}/review")
    public KycResponse review(@PathVariable String userId) {
        return service.reviewKyc(userId);
    }
}