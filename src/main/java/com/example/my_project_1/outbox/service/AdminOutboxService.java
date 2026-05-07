package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Transactional
@Service
@RequiredArgsConstructor
public class AdminOutboxService {
    private final Clock clock;
    private final OutboxRepository outboxRepository;
    private final OutboxPublisher outboxPublisher;

    public void retry(Long id) {
        OutboxEvent event = findRetryableEvent(id);
        event.resetForRetry(LocalDateTime.now(clock));
    }

    public void retryNow(Long id) {
        OutboxEvent event = findRetryableEvent(id);
        event.resetForRetry(LocalDateTime.now(clock));

        outboxPublisher.requestProcessing(event.getId());
    }

    private OutboxEvent findRetryableEvent(Long id) {
        OutboxEvent event = outboxRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));

        if (event.getStatus() != OutboxStatus.FAILED && event.getStatus() != OutboxStatus.DEAD) {
            throw new CustomException(ErrorCode.OUTBOX_RETRY_NOT_ALLOWED);
        }

        return event;
    }
}
