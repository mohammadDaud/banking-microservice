package com.bank.accs.model;

import jakarta.persistence.Column;
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

    @Column(name = "per_transaction_limit")
    private BigDecimal perTransactionLimit;

    @Column(name = "daily_limit")
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit")
    private BigDecimal monthlyLimit;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}