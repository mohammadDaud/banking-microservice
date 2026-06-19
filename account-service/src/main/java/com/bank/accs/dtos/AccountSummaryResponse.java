package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountSummaryResponse {

    private Integer totalAccounts;

    private BigDecimal totalBalance;
}