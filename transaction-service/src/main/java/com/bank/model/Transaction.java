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

    /*
     * Optimistic locking version.
     *
     * Hibernate automatically:
     * - starts with version 0 for a new row
     * - increments it on every update
     * - prevents two checker requests from updating the same
     *   PENDING_APPROVAL transaction at the same time.
     */
    @Version
    @Column(nullable = false)
    private long version = 0L;

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

    private LocalDateTime updatedAt;

    private String updatedBy;

    /*
     * Maker-checker fields
     */

    @Column(name = "maker_id")
    private String makerId;

    @Column(name = "checker_id")
    private String checkerId;

    @Column(name = "checker_action_at")
    private LocalDateTime checkerActionAt;

    @Column(name = "checker_remarks", length = 1000)
    private String checkerRemarks;

    /*
     * BRE decision details
     */

    @Column(name = "rule_code")
    private String ruleCode;

    @Column(name = "rule_reason", length = 1000)
    private String ruleReason;

    /* reversal failure */

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "reversal_attempted_at")
    private LocalDateTime reversalAttemptedAt;

    @Column(name = "reversal_completed_at")
    private LocalDateTime reversalCompletedAt;
}