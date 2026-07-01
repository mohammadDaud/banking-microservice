package com.bank.us.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "nominees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nominee {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "userId"
    )
    private UserProfile userProfile;

    private String nomineeName;

    private String relationship;

    private String mobileNumber;

    private Double percentageShare;

    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}