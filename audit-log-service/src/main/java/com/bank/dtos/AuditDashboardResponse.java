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

    private long totalAuditLogs;

    private long todayAuditLogs;

    private long loginSuccess;

    private long loginFailed;

    private long userRegistrations;

    private long accountsCreated;

    private long kycApproved;

    private long beneficiaryApproved;

    private long successfulTransactions;

    private long failedTransactions;

}