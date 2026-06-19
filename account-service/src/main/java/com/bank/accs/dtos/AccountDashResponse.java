package com.bank.accs.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AccountDashResponse {

    private String id;

    private String accountNumber;

    private String accountType;

    private String status;

    private LocalDateTime createdAt;
}
