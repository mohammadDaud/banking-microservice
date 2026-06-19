package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDashResponse {

    private String id;

    private String referenceNumber;

    private BigDecimal amount;

    private String transactionType;

    private String status;

    private LocalDateTime transactionDate;
}
