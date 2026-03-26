package com.example.my_project_1.outbox.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OutboxSavedEvent {
    private final Long outboxId;
}
