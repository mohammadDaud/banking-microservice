package com.bank.dtos;

import lombok.Data;

@Data
public class FieldOperatorMappingRequest {

    private Long operatorId;

    private String requestedBy;
}