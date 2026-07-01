package com.bank.us.model;

import com.bank.us.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private String userId;

    private String username;

    private String email;

    private String firstName;

    private String middleName;

    private String lastName;

    private String mobileNumber;

    private LocalDate dateOfBirth;

    private String gender;

    private String nationality;

    private String maritalStatus;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime deletedAt;

    private String deletedBy;

    private LocalDateTime createdAt;
    private String createdBy;

    private String updatedBy;
    private LocalDateTime updatedAt;
}