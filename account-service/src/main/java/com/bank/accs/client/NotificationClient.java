package com.bank.accs.client;

import com.bank.accs.dtos.NotificationRequest;
import com.bank.accs.dtos.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    NotificationResponse createNotification(@RequestBody NotificationRequest request);
}