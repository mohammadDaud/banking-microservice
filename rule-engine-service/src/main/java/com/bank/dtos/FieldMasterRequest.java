package com.bank.dtos;

import com.bank.enums.FieldDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FieldMasterRequest {

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Data type is required")
    private FieldDataType dataType;

    private String description;

    private Boolean isActive = true;

    /*
     * Controller sets this from gateway header.
     * Angular must not send this value.
     */
    private String requestedBy;
}