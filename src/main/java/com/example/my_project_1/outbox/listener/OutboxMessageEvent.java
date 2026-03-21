package com.example.my_project_1.outbox.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutboxMessageEvent {
    private Long outboxId;
}
