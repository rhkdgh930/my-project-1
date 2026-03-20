package com.example.my_project_1.outbox.handler;

import com.example.my_project_1.outbox.domain.OutboxEventType;

public interface OutboxHandler {
    OutboxEventType getEventType();

    void handle(String payload);
}
