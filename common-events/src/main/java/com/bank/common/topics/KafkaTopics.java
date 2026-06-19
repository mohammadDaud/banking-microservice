package com.bank.common.topics;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String
            EMAIL_NOTIFICATION_TOPIC =
            "email-notification-topic";

    public static final String
            NOTIFICATION_TOPIC =
            "notification-topic";

    public static final String
            USER_REGISTRATION_TOPIC =
            "user-registration-topic";

    public static final String
            AUDIT_LOG_TOPIC =
            "audit-log-topic";

    /*public static final String
            ACCOUNT_NUMBER_GENERATED_TOPIC =
            "account-number-generated-topic";*/
}