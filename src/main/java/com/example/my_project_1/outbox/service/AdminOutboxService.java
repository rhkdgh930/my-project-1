package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.domain.OutboxEvent;
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

    public void retry(Long id) {
        OutboxEvent event = outboxRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));

        event.resetForRetry(LocalDateTime.now(clock));
    }
}
