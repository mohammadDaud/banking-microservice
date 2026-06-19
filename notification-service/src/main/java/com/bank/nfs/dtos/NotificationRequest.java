package com.bank.nfs.dtos;

import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import lombok.Data;

@Data
public class NotificationRequest {

    private String userId;

    private String title;

    private String message;

    private NotificationType type;

    private NotificationPriority priority;
}