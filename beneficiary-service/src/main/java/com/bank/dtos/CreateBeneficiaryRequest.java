package com.bank.dtos;

import lombok.Data;

@Data
public class CreateBeneficiaryRequest {

    private String customerId;
    /*
     * Temporary approach.
     * Later get makerId from JWT token / SecurityContext.
     */
    private String makerId;

    private String beneficiaryName;

    private String accountNumber;

    private String bankName;

    private String ifscCode;

    private String nickname;
}
