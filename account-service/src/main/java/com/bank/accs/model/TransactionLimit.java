package com.bank.accs.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_limits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLimit {

    @Id
    private String id;

    private String customerId;

    private String accountNumber;

    private BigDecimal perTransactionLimit;

    private BigDecimal dailyLimit;

    private BigDecimal monthlyLimit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}