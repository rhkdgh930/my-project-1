package com.example.my_project_1.outbox.domain;

public enum OutboxEventType {
    POST_CREATED,
    POST_UPDATED,
    DORMANCY_NOTIFY,
    USER_ACCOUNT_CHANGED,
    EMAIL_VERIFICATION,
    PASSWORD_RESET
}
