package com.bank.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TransactionReportFilterRequest {

    private LocalDate fromDate;

    private LocalDate toDate;

    private String status;

    private String type;
}