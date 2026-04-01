package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEventType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OutboxEventSnapshot {
    private final Long id;
    private final OutboxEventType eventType;
    private final String payload;
}
