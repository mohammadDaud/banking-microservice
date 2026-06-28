package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDashboardResponse {

    private long totalTransactions;

    private long successfulTransactions;

    private long failedTransactions;

    private long pendingTransactions;

    private long pendingApprovalTransactions;

    private long autoApprovedTransactions;

    private long todayTransactions;

    private BigDecimal todayTransferAmount;

    private BigDecimal monthlyTransferAmount;

    private BigDecimal averageTransactionAmount;

    private BigDecimal highestTransactionAmount;

    private BigDecimal lowestTransactionAmount;

}