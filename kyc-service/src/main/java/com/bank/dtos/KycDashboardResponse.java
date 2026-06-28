package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDashboardResponse {

    private long totalKyc;

    private long pendingKyc;

    private long underReviewKyc;

    private long approvedKyc;

    private long rejectedKyc;

    private long submittedToday;

    private double approvalRate;
}