package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxEventManager {

    private final Clock clock;
    private final OutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutboxEventSnapshot claim(Long outboxId) {
        LocalDateTime now = LocalDateTime.now(clock);

        int updated = outboxRepository.claim(outboxId, now);
        if (updated == 0) {
            return null;
        }

        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));

        return new OutboxEventSnapshot(event.getId(), event.getEventType(), event.getPayload());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));
        event.markSuccess(LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFail(Long outboxId, Exception e) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));
        event.markFail(e, LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDead(Long outboxId, String reason) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));
        event.markDead(reason, LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetForRetry(Long outboxId) {
        OutboxEvent event = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));
        event.resetForRetry(LocalDateTime.now(clock));
    }
}