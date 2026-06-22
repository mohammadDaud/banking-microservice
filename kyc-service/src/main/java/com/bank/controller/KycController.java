package com.bank.controller;

import com.bank.dtos.CreateKycRequest;
import com.bank.dtos.KycEligibilityResponse;
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
    @PutMapping(value = "/{userId}/resubmit",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycResponse resubmitKyc(
            @PathVariable String userId,
            @RequestParam String panNumber,
            @RequestParam String aadhaarNumber,
            @RequestPart MultipartFile panDocument,
            @RequestPart MultipartFile aadhaarDocument) {

        return service.resubmitKyc(
                userId,
                panNumber,
                aadhaarNumber,
                panDocument,
                aadhaarDocument
        );
    }
    @GetMapping("/{userId}")
    public KycResponse getKyc(@PathVariable String userId) {
        return service.getKyc(userId);
    }

    @GetMapping("/{userId}/eligibility")
    public KycEligibilityResponse checkEligibility(@PathVariable String userId) {
        return service.checkEligibility(userId);
    }
}