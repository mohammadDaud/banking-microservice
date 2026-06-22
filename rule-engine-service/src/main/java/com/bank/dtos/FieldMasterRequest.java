package com.bank.dtos;

import com.bank.enums.FieldDataType;
import lombok.Data;

@Data
public class FieldMasterRequest {

    private String fieldName;

    private String displayName;

    private FieldDataType dataType;

    private String description;

    private Boolean isActive = true;

    // Temporary. Later get this from JWT.
    private String requestedBy;
}