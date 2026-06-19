package com.bank.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountTransactionRequest {

    private String customerId;

    private String accountNumber;

    private BigDecimal amount;

    private String description;
}