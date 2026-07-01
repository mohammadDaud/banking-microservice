package com.bank.model;

import com.bank.enums.BeneficiaryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {

    @Id
    private String id;

    private String customerId;

    private String beneficiaryName;

    private String accountNumber;

    private String bankName;

    private String ifscCode;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private BeneficiaryStatus status;

    // User/customer/employee who submitted request
    @Column(nullable = false)
    private String makerId;

    // Admin/checker who approved or rejected request
    private String checkerId;

    @Column(length = 500)
    private String checkerRemark;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;

    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;
}