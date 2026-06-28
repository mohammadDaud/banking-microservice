package com.bank.common.events;

import com.bank.common.enums.EventSource;
import com.bank.common.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private String eventId;

    private String correlationId;

    private String requestId;

    private String serviceName;

    private EventSource source;

    private String userId;

    private String username;

    private String email;

    private EventStatus status;

    private LocalDateTime createdAt;

}