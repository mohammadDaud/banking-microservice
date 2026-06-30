package com.bank.model;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
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

    /*================ Event Metadata =================*/

    @Column(unique = true)
    private String eventId;


    private String correlationId;

    private String requestId;

    private String serviceName;

    @Enumerated(EnumType.STRING)
    private EventSource source;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    /*================ User Information =================*/

    private String userId;

    private String username;

    private String role;

    /*================ Audit Information =================*/

    @Enumerated(EnumType.STRING)
    private AuditModule module;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String entityId;

    private String entityType;

    /*================ Request Information =================*/

    private String requestMethod;

    private String requestUri;

    private String ipAddress;

    /*================ Description =================*/

    @Column(length = 3000)
    private String description;

    /*================ Timestamp =================*/

    private LocalDateTime createdAt;

}