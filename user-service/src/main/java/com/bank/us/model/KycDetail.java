package com.bank.us.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "kyc_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDetail {

    @Id
    private String id;

    private String userId;

    private String panNumber;

    private String aadhaarNumber;

    private String passportNumber;

    private String verificationStatus;
}