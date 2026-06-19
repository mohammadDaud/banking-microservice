package com.bank.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private String id;

    private String userId;

    private String username;

    private String role;

    private String module;

    private String action;

    private String description;

    private LocalDateTime createdAt;
}