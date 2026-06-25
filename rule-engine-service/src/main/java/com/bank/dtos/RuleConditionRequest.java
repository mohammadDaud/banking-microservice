package com.bank.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuleConditionRequest {

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotBlank(message = "Operator is required")
    private String operatorShortName;

    @NotBlank(message = "Condition value is required")
    private String conditionValue;

    @NotNull(message = "Condition sequence order is required")
    private Integer sequenceOrder;
}