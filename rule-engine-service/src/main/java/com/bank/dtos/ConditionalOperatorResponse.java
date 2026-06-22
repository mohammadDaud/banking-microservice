package com.bank.dtos;

import com.bank.enums.OperatorCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConditionalOperatorResponse {

    private Long id;
    private String shortName;
    private String symbol;
    private String displayName;
    private String description;
    private OperatorCategory category;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}