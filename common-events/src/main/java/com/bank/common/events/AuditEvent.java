package com.bank.common.events;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    private String eventId;

    private String correlationId;

    private String requestId;

    private String serviceName;

    private EventSource source;

    private String requestUri;

    private String requestMethod;

    private EventStatus status;

    private String userId;

    private String username;

    private String role;

    private String module;

    private String action;

    private String entityId;

    private String entityType;

    private String description;

    private String ipAddress;

    private LocalDateTime createdAt;
}