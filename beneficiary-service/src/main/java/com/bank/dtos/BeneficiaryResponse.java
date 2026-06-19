package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BeneficiaryResponse {

    private String id;

    private String beneficiaryName;

    private String accountNumber;

    private String bankName;

    private String ifscCode;

    private String nickname;

    private String status;
}