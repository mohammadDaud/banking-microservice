package com.bank.model;

import com.bank.enums.KycStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycProfile {

    @Id
    private String id;

    private String userId;

    private String panNumber;

    private String aadhaarNumber;

    private String panDocumentPath;

    private String aadhaarDocumentPath;

    @Enumerated(EnumType.STRING)
    private KycStatus kycStatus;

    private String remarks;

    // ADD BELOW
    @Column(nullable = false)
    private String makerId;

    private String checkerId;

    @Column(length = 500)
    private String checkerRemark;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}