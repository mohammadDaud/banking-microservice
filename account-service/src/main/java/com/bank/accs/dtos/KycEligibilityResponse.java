package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KycEligibilityResponse {

    private String userId;
    private boolean eligible;
    private String status;
    private String message;
}