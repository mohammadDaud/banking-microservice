package com.bank.us.service;


import com.bank.us.dtos.DashboardMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardPushService {

    private final SimpMessagingTemplate messagingTemplate;
    public static final String DASHBOARD_TOPIC = "/topic/dashboard";

    /**
     * Push dashboard update to all connected clients.
     */
    public void push(DashboardMessage message) {
        if (message == null) {
            log.warn("Dashboard message is null.");
            return;
        }
        log.info("Sending dashboard update : {}", message);
        try {
            messagingTemplate.convertAndSend(DASHBOARD_TOPIC, message);
        } catch (Exception ex) {
            log.error("Unable to push dashboard message.", ex);
        }
    }

}