package com.bank.accs.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDashboardResponse {

    private long totalAccounts;

    private long activeAccounts;

    private long inactiveAccounts;

    private long savingsAccounts;

    private long currentAccounts;

    private BigDecimal totalBankBalance;

    private BigDecimal averageAccountBalance;

    private long accountsCreatedToday;

}