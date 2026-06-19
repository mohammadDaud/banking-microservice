package com.bank.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateNomineeRequest {
    private String customerId;
    private String accountNumber;
    private String nomineeName;
    private String relationship;
    private LocalDate dateOfBirth;
    private String mobileNumber;
    private String email;
    private String address;
    private Integer percentageShare;
}