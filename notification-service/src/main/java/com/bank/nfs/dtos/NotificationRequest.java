package com.bank.nfs.dtos;

import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotBlank
    private String userId;
    @NotBlank
    private String title;
    @NotBlank
    private String message;

    private NotificationType type;

    private NotificationPriority priority;
}