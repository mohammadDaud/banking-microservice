package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleConditionResponse {

    private Long id;

    private String fieldName;
    private String displayName;
    private String dataType;

    private String operatorShortName;
    private String operatorSymbol;

    private String conditionValue;
    private Integer sequenceOrder;
}