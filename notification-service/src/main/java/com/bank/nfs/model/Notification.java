package com.bank.nfs.model;

import com.bank.common.enums.EventStatus;
import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    private String eventId;

    private String correlationId;

    private String requestId;

    private String serviceName;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private String userId;

    private String title;

    private String message;

    private Boolean readFlag;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationPriority priority;

    private LocalDateTime createdAt;
}