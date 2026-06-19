package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyTransactionResponse {

    private String month;

    private Double amount;
}