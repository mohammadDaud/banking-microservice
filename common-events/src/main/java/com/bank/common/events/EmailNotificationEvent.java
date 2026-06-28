package com.bank.common.events;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationEvent {

    private String eventId;

    private String correlationId;

    private String requestId;

    private String serviceName;

    private EventSource source;

    private String to;

    private String cc;

    private String bcc;

    private String subject;

    private String body;

    private String templateName;

    private EventStatus status;

    private LocalDateTime createdAt;

}