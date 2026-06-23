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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycResponse createKyc(
            @RequestHeader("X-User-Id") String loggedInUserId,
            @RequestParam String panNumber,
            @RequestParam String aadhaarNumber,
            @RequestPart MultipartFile panDocument,
            @RequestPart MultipartFile aadhaarDocument) {

        return service.createKyc(
                loggedInUserId,
                panNumber,
                aadhaarNumber,
                panDocument,
                aadhaarDocument
        );
    }

    @PutMapping(
            value = "/resubmit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public KycResponse resubmitKyc(
            @RequestHeader("X-User-Id") String loggedInUserId,
            @RequestParam String panNumber,
            @RequestParam String aadhaarNumber,
            @RequestPart MultipartFile panDocument,
            @RequestPart MultipartFile aadhaarDocument) {

        return service.resubmitKyc(
                loggedInUserId,
                panNumber,
                aadhaarNumber,
                panDocument,
                aadhaarDocument
        );
    }

    @GetMapping("/me")
    public KycResponse getMyKyc(
            @RequestHeader("X-User-Id") String loggedInUserId) {

        return service.getKyc(loggedInUserId);
    }

    /*
     * Internal endpoint used by account-service / beneficiary-service.
     * Later restrict this endpoint to your Docker internal network.
     */
    @GetMapping("/{userId}/eligibility")
    public KycEligibilityResponse checkEligibility(
            @PathVariable String userId) {

        return service.checkEligibility(userId);
    }
}