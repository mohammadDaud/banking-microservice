package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionLimitResponse {

    private String accountNumber;

    private BigDecimal perTransactionLimit;

    private BigDecimal dailyLimit;

    private BigDecimal monthlyLimit;
}