package com.bank.common.events;

import com.bank.common.enums.EventSource;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDeletedEvent {


    /* ---------- Common Event Metadata ---------- */

    private String eventId;

    private String correlationId;

    private String requestId;

    private String serviceName;

    private EventSource source;

    private LocalDateTime createdAt;

    /* ---------- Customer Details ---------- */

    private String userId;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String mobileNumber;

    /* ---------- Deletion Details ---------- */

    private String deletedBy;

    private LocalDateTime deletedAt;

    /* ---------- Accounts ---------- */

    private List<String> accountNumbers;

}