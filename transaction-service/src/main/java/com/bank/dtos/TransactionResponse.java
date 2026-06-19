package com.bank.dtos;

import com.bank.enums.TransactionStatus;
import com.bank.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private String transactionReference;

    private String sourceAccount;

    private String destinationAccount;

    private TransactionType transactionType;

    private TransactionStatus transactionStatus;

    private BigDecimal amount;

    private String description;

    private LocalDateTime transactionDate;
}