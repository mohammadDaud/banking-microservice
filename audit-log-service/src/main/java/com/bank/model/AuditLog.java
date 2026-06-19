package com.bank.model;

import com.bank.enums.AuditAction;
import com.bank.enums.AuditModule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String userId;

    private String username;

    private String role;

    @Enumerated(EnumType.STRING)
    private AuditModule module;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String entityId;

    private String entityType;

    @Column(length = 2000)
    private String description;

    private String ipAddress;

    private LocalDateTime createdAt;
}