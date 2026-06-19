package com.bank.nfs.dtos;

import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private String id;

    private String title;

    private String message;

    private Boolean readFlag;

    private NotificationType type;

    private NotificationPriority priority;

    private LocalDateTime createdAt;
}