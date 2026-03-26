package com.example.my_project_1.outbox.domain;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DEAD
}
