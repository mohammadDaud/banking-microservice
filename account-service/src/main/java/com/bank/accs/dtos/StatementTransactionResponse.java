package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class StatementTransactionResponse {

    private String transactionReference;

    private String sourceAccount;

    private String destinationAccount;

    private String transactionType;

    private String transactionStatus;

    private BigDecimal amount;

    private String description;

    private LocalDateTime transactionDate;
}