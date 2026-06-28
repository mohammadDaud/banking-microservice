package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryDashboardResponse {

    private long totalBeneficiaries;

    private long pendingBeneficiaries;

    private long approvedBeneficiaries;

    private long rejectedBeneficiaries;

    private long addedToday;
}