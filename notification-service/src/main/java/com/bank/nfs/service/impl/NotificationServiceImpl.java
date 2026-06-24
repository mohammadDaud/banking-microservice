package com.bank.nfs.service.impl;

import com.bank.nfs.dtos.NotificationRequest;
import com.bank.nfs.dtos.NotificationResponse;
import com.bank.nfs.enums.NotificationPriority;
import com.bank.nfs.enums.NotificationType;
import com.bank.nfs.exception.NotificationNotFoundException;
import com.bank.nfs.model.Notification;
import com.bank.nfs.repository.NotificationRepository;
import com.bank.nfs.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    @Override
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification =
                Notification.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(request.getUserId())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .type(request.getType()==null?NotificationType.KYC:request.getType())
                        .priority(request.getPriority()==null?NotificationPriority.LOW:request.getPriority())
                        .readFlag(false)
                        .createdAt(LocalDateTime.now())
                        .build();
        repository.save(notification);
        return map(notification);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(String userId) {
        return repository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(String notificationId) {

        Notification notification =
                repository.findById(notificationId)
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        "Notification not found"));

        notification.setReadFlag(true);
        repository.save(notification);
        return map(notification);
    }

    @Override
    public void deleteNotification(String notificationId) {
        Notification notification =
                repository.findById(notificationId)
                        .orElseThrow(() ->
                                new NotificationNotFoundException("Notification not found"));

        repository.delete(notification);
    }

    @Override
    public Long getUnreadCount(String userId) {
        return repository.countByUserIdAndReadFlag(userId,false);
    }

    @Override
    public List<NotificationResponse> getRecentNotifications(String userId) {
        return repository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public void markAllRead(String userId) {
        repository.markAllRead(userId);
    }

    @Override
    public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
       return repository.findByUserIdOrderByCreatedAtDesc(userId,pageable)
                    .map(this::map);
    }

    private NotificationResponse map(Notification notification) {
        return NotificationResponse
                .builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .priority(notification.getPriority())
                .readFlag(notification.getReadFlag())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}