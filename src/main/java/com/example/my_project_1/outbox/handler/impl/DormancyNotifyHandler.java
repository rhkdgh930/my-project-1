package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormancyNotifyHandler implements OutboxHandler {

    private final EmailService emailService;

    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.DORMANCY_NOTIFY;
    }

    @Override
    public void handle(String payload) {
        DormancyNotifyOutboxEvent event =
                DataSerializer.deserialize(payload, DormancyNotifyOutboxEvent.class);

        emailService.sendDormancyWarning(
                event.getEmail(),
                event.getNickname()
        );

        log.debug("[OUTBOX][DORMANCY][SUCCESS] userId={}", event.getUserId());
    }
}
