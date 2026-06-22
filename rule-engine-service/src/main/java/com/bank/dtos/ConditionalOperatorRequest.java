package com.bank.dtos;

import com.bank.enums.OperatorCategory;
import lombok.Data;

@Data
public class ConditionalOperatorRequest {

    private String shortName;

    private String symbol;

    private String displayName;

    private String description;

    private OperatorCategory category;

    private Boolean isActive = true;

    // Temporary. Later get this from JWT.
    private String requestedBy;
}