package com.bank.model;

import com.bank.enums.TransactionStatus;
import com.bank.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id;

    @Column(unique = true)
    private String transactionReference;

    private String customerId;

    private String sourceAccount;

    private String destinationAccount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    private BigDecimal amount;

    private String description;

    private LocalDateTime transactionDate;

    private LocalDateTime createdAt;
}