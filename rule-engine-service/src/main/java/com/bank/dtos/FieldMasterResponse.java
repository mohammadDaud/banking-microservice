package com.bank.dtos;

import com.bank.enums.FieldDataType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FieldMasterResponse {

    private Long id;
    private String fieldName;
    private String displayName;
    private FieldDataType dataType;
    private String description;
    private Boolean isActive;
    private List<String> allowedOperators;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}