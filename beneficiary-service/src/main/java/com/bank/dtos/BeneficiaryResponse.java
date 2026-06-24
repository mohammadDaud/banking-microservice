package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BeneficiaryResponse {

    private String id;

    private String customerId;

    private String beneficiaryName;

    private String accountNumber;

    private String bankName;

    private String ifscCode;

    private String nickname;

    private String status;

    private String makerId;

    private String checkerId;

    private String checkerRemarks;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}