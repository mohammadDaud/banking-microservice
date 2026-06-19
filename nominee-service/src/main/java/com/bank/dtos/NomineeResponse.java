package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class NomineeResponse {
    private String id;
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