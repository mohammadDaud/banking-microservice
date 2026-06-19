package com.bank.accs.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionLimitRequest {

    private BigDecimal perTransactionLimit;

    private BigDecimal dailyLimit;

    private BigDecimal monthlyLimit;
}