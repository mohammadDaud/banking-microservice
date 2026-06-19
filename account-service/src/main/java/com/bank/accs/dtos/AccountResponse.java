package com.bank.accs.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String accountNumber;

    private String customerId;

    private String accountType;

    private String accountStatus;

    private String currency;

    private String branchCode;
}