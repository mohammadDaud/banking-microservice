package com.bank.nfs.service;

import com.bank.nfs.dtos.NotificationDashboardResponse;
import com.bank.nfs.dtos.NotificationRequest;
import com.bank.nfs.dtos.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(NotificationRequest request);
    List<NotificationResponse> getUserNotifications(String userId);
    NotificationResponse markAsRead(String notificationId);
    void deleteNotification(String notificationId);
    Long getUnreadCount(String userId);
    List<NotificationResponse> getRecentNotifications(String userId);
    void markAllRead(String userId);
    Page<NotificationResponse> getNotifications(String userId,Pageable pageable);

    NotificationDashboardResponse getDashboardStats();
}