package com.bank.us.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDetail {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "userId"
    )
    private UserProfile userProfile;

    private String panNumber;

    private String aadhaarNumber;

    private String passportNumber;

    private String verificationStatus;

    private LocalDateTime verifiedAt;

    private String verifiedBy;

    private String remarks;
}