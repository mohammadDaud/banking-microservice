package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionReportSummaryResponse {

    private Long todayTransactions;

    private BigDecimal todayAmount;

    private Long monthlyTransactions;

    private BigDecimal monthlyAmount;

    private Long successTransactions;

    private Long failedTransactions;
}