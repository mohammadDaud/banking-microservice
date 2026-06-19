package com.bank.nfs.controller;

import com.bank.common.events.EmailNotificationEvent;
import com.bank.nfs.dtos.NotificationRequest;
import com.bank.nfs.dtos.NotificationResponse;
import com.bank.nfs.service.EmailService;
import com.bank.nfs.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    private final NotificationService service;

    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailNotificationEvent request) {
        emailService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping
    public NotificationResponse createNotification(@RequestBody NotificationRequest request) {
        return service.createNotification(request);
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getUserNotifications(@PathVariable String userId) {
        return service.getUserNotifications(userId);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable String notificationId) {
        return service.markAsRead(notificationId);
    }

    @PutMapping("/user/{userId}/read-all")
    public void markAllRead(@PathVariable String userId) {
        service.markAllRead(userId);
    }

    @DeleteMapping("/{notificationId}")
    public void deleteNotification(@PathVariable String notificationId) {
        service.deleteNotification(notificationId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public Long unreadCount(@PathVariable String userId) {
        return service.getUnreadCount(userId);
    }

    @GetMapping("/user/{userId}/recent")
    public List<NotificationResponse> recentNotifications(@PathVariable String userId) {
        return service.getRecentNotifications(userId);
    }

    @GetMapping("/user/{userId}/page")
    public Page<NotificationResponse> getNotifications(@PathVariable String userId,Pageable pageable) {
        return service.getNotifications(userId,pageable);
    }

}

