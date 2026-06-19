package com.bank.dtos;

import com.bank.enums.KycStatus;
import lombok.Builder;
import lombok.Data;

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
}