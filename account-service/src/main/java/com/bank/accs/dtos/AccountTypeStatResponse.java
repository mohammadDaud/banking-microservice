package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountTypeStatResponse {

    private String accountType;

    private Long count;
}