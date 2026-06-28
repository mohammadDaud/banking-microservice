package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDashboardResponse {

    private long totalLogs;

    private long todayLogins;

    private long todayTransfers;

    private long todayFailedTransfers;

    private long todayAccountsCreated;

    private long todayBeneficiariesAdded;

    private long todayKycApproved;
}