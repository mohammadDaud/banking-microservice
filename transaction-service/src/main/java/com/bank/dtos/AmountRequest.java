package com.bank.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AmountRequest {

    private BigDecimal amount;
}