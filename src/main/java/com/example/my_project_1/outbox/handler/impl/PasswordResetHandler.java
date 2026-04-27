package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisPasswordResetTokenService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.user.event.PasswordResetOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetHandler implements OutboxHandler {

    private final RedisPasswordResetTokenService redisPasswordResetTokenService;
    private final EmailService emailService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.PASSWORD_RESET;
    }

    @Override
    public void handle(String payload) {
        PasswordResetOutboxEvent event =
                DataSerializer.deserialize(payload, PasswordResetOutboxEvent.class);

        if (event == null || event.getEmail() == null || event.getRawToken() == null || event.getResetLink() == null) {
            throw new IllegalArgumentException("Invalid PASSWORD_RESET payload");
        }

        redisPasswordResetTokenService.saveToken(event.getRawToken(), event.getEmail());
        emailService.sendPasswordResetLink(event.getEmail(), event.getResetLink());

        log.debug("[OUTBOX][PASSWORD_RESET][SUCCESS] email={}", event.getEmail());
    }
}