package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AccountStatementResponse {

    private String accountNumber;

    private String customerId;

    private String accountType;

    private BigDecimal availableBalance;

    private BigDecimal ledgerBalance;

    private List<StatementTransactionResponse> transactions;
}