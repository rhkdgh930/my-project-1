package com.example.my_project_1.outbox.domain;

public enum OutboxEventType {
    POST_CREATED,
    POST_UPDATED,
    POST_DELETED,
    DORMANCY_NOTIFY,
    USER_ACCOUNT_CHANGED
}
