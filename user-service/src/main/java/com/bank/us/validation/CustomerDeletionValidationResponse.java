package com.bank.us.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDeletionValidationResponse {

    private boolean allowed;

    private String serviceName;

    private LocalDateTime validatedAt;

    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

}