package com.bank.dtos;

import lombok.Data;

@Data
public class BeneficiaryApprovalRequest {
    /*
     * Temporary approach.
     * Later get checkerId from JWT token / SecurityContext.
     */
    private String checkerId;
    private String remark;
}