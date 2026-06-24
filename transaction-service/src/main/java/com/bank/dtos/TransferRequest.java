package com.bank.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    private String customerId;

    private String fromAccount;

    private String beneficiaryId;

    private BigDecimal amount;

    private String description;
}