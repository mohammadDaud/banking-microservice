package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateAccountResponse {

    private String accountNumber;

    private String customerId;

    private String accountStatus;
}
