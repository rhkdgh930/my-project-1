package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserAccountChangedHandler implements OutboxHandler {

    private final RedisUserContextService redisUserContextService;
    private final RedisTokenService redisTokenService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.USER_ACCOUNT_CHANGED;
    }

    @Override
    public void handle(String payload) {
        UserAccountChangedOutboxEvent event =
                DataSerializer.deserialize(payload, UserAccountChangedOutboxEvent.class);

        if (event == null || event.getType() == null) {
            throw new IllegalArgumentException("Invalid USER_ACCOUNT_CHANGED payload");
        }

        Long userId = event.getUserId();
        UserAccountChangedType type = event.getType();

        if (type.shouldEvictCache()) {
            redisUserContextService.evict(userId);
        }

        if (type.shouldInvalidateToken()) {
            redisTokenService.deleteRefreshTokenHash(userId);
        }

        log.debug("[OUTBOX][USER_ACCOUNT_CHANGED][SUCCESS] userId={} type={}", userId, type);
    }
}
