package com.bank.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventMetadata {

    private String correlationId;

    private String requestId;

    private LocalDateTime createdAt;

}