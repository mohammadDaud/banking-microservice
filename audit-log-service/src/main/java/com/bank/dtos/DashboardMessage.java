package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMessage {

    /**
     * TRANSACTION
     * AUDIT
     * KYC
     * BENEFICIARY
     * USER
     * ACCOUNT
     * NOTIFICATION
     */
    private String type;

    /**
     * CREATED
     * UPDATED
     * APPROVED
     * REJECTED
     * SUCCESS
     * FAILED
     */
    private String action;

    /**
     * Event Time
     */
    private LocalDateTime timestamp;

    /**
     * Actual Event Data
     */
    private Object data;

}