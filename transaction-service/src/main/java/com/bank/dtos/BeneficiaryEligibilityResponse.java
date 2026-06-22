package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BeneficiaryEligibilityResponse {

    private String beneficiaryId;
    private String customerId;
    private boolean eligible;
    private String status;
    private String message;
}