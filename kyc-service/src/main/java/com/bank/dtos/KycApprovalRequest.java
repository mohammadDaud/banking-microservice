package com.bank.dtos;

import lombok.Data;

@Data
public class KycApprovalRequest {
    // Temporary: later extract from JWT
    private String checkerId;
    private String remark;
}