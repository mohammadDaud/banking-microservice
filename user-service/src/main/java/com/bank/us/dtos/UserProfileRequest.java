package com.bank.us.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileRequest {

    private String firstName;

    private String middleName;

    private String lastName;

    private String mobileNumber;

    private LocalDate dateOfBirth;

    private String gender;

    private String nationality;

    private String maritalStatus;
}