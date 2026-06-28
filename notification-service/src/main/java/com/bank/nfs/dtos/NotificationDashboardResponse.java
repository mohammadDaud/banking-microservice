package com.bank.nfs.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDashboardResponse {

    private long totalNotifications;

    private long unreadNotifications;

    private long readNotifications;

    private long notificationsToday;

}