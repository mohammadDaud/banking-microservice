package com.bank.dtos;

import lombok.Data;

@Data
public class CreateKycRequest {

    private String userId;

    private String panNumber;

    private String aadhaarNumber;

    private String panDocumentUrl;

    private String aadhaarDocumentUrl;
}