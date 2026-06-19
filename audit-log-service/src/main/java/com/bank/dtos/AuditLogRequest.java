package com.bank.dtos;

import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    private String userId;

    private String username;

    private String role;

    private AuditModule module;

    private AuditAction action;

    private String entityId;

    private String entityType;

    private String description;

    private String ipAddress;
}