package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisEmailVerificationService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.user.event.EmailVerificationOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationHandler implements OutboxHandler {

    private final RedisEmailVerificationService redisEmailVerificationService;
    private final EmailService emailService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.EMAIL_VERIFICATION;
    }

    @Override
    public void handle(String payload) {
        EmailVerificationOutboxEvent event =
                DataSerializer.deserialize(payload, EmailVerificationOutboxEvent.class);

        if (event == null || event.getEmail() == null || event.getCode() == null) {
            throw new IllegalArgumentException("Invalid EMAIL_VERIFICATION payload");
        }

        redisEmailVerificationService.saveCode(event.getEmail(), event.getCode());
        emailService.sendVerificationCode(event.getEmail(), event.getCode());

        log.debug("[OUTBOX][EMAIL_VERIFICATION][SUCCESS] email={}", event.getEmail());
    }
}