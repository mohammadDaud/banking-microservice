package com.bank.us.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}