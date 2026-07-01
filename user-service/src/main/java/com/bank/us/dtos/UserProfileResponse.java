package com.bank.us.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

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

    private Boolean deleted;

    private LocalDateTime deletedAt;

    private String deletedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}