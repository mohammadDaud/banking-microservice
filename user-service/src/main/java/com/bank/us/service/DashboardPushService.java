package com.bank.us.service;


import com.bank.us.dtos.DashboardMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push dashboard update to all connected clients.
     */
    public void push(DashboardMessage message) {

        messagingTemplate.convertAndSend(
                "/topic/dashboard",
                message
        );

    }

}