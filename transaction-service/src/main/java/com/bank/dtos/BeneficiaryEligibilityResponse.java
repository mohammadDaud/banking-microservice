package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryEligibilityResponse {

    private String beneficiaryId;

    private String customerId;

    private String accountNumber;

    private String bankName;

    private String ifscCode;

    private boolean eligible;

    private String status;

    private String message;
}