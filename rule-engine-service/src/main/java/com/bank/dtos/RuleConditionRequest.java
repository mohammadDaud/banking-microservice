package com.bank.dtos;

import lombok.Data;

@Data
public class RuleConditionRequest {

    private String fieldName;

    private String operatorShortName;

    private String conditionValue;

    private Integer sequenceOrder;
}