package com.bank.common.util;

import java.time.LocalDateTime;
import java.util.UUID;

public final class EventMetadataUtil {

    private EventMetadataUtil() {
    }

    public static String eventId() {
        return UUID.randomUUID().toString();
    }

    public static String requestId() {
        return UUID.randomUUID().toString();
    }

    public static LocalDateTime createdAt() {
        return LocalDateTime.now();
    }
}