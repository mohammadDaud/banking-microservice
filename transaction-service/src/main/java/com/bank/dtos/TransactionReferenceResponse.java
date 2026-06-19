package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionReferenceResponse {
    private String referenceNumber;
}