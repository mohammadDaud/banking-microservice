package com.bank.dtos;

import com.bank.enums.KycStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KycResponse {

    private String id;

    private String userId;

    private String panNumber;

    private String aadhaarNumber;

    private String panDocumentUrl;

    private String aadhaarDocumentUrl;

    private KycStatus kycStatus;

    private String remarks;

    // Maker–Checker response fields
    private String makerId;

    private String checkerId;

    private String checkerRemark;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;
}