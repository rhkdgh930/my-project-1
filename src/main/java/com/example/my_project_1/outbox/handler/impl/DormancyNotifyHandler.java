package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.EmailService;
import com.example.my_project_1.auth.service.RedisDormancyHistoryService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.handler.OutboxHandler;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormancyNotifyHandler implements OutboxHandler {

    private final EmailService emailService;
    private final RedisDormancyHistoryService redisDormancyHistoryService;


    @Override
    public OutboxEventType getEventType() {
        return OutboxEventType.DORMANCY_NOTIFY;
    }

    @Override
    public void handle(String payload) {

        DormancyNotifyOutboxEvent event =
                DataSerializer.deserialize(payload, DormancyNotifyOutboxEvent.class);

        Long userId = event.getUserId();

        boolean executed = redisDormancyHistoryService.executeOnce(
                userId,
                Duration.ofDays(30),
                () -> emailService.sendDormancyWarning(
                        event.getEmail(),
                        event.getNickname()
                )
        );

        if (executed) {
            log.info("[OUTBOX][DORMANCY][SUCCESS] userId={}", userId);
        } else {
            log.info("[OUTBOX][DORMANCY][SKIP_DUPLICATE] userId={}", userId);
        }
    }
}
