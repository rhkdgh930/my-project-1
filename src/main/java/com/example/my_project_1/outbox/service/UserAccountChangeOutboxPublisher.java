package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAccountChangeOutboxPublisher {
    private final OutboxPublisher outboxPublisher;

    public void publish(Long userId, UserAccountChangedType type) {
        UserAccountChangedOutboxEvent payload = new UserAccountChangedOutboxEvent(userId, type);
        outboxPublisher.publish(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                DataSerializer.serialize(payload),
                "USER_ACCOUNT_CHANGED:%d:%s:%s".formatted(userId, type.name(), UUID.randomUUID())
        );
    }
}
