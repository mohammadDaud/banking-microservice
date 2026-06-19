package com.bank.accs.dtos;

import com.bank.accs.model.enums.AccountType;
import com.bank.accs.model.enums.CurrencyType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    private String customerId;

    private AccountType accountType;

    private CurrencyType currency;

    private String branchCode;

    private BigDecimal openingBalance;
}