package com.bank.dtos;

import com.bank.enums.OperatorCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConditionalOperatorRequest {

    @NotBlank(message = "Operator short name is required")
    private String shortName;

    @NotBlank(message = "Operator symbol is required")
    private String symbol;

    @NotBlank(message = "Operator display name is required")
    private String displayName;

    private String description;

    @NotNull(message = "Operator category is required")
    private OperatorCategory category;

    private Boolean isActive = true;

    /*
     * Controller sets this from gateway header.
     * Angular must not send this value.
     */
    private String requestedBy;
}