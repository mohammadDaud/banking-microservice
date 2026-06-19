package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse {

    private String accountNumber;

    private BigDecimal availableBalance;

    private BigDecimal ledgerBalance;
}