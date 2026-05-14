package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.outbox.service.response.AdminOutboxDetailResponse;
import com.example.my_project_1.outbox.service.response.AdminOutboxResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public PageResponse<AdminOutboxResponse> findPage(OutboxStatus status, Pageable pageable) {
        Page<OutboxEvent> page = status == null
                ? outboxRepository.findAll(pageable)
                : outboxRepository.findAllByStatus(status, pageable);

        return PageResponse.of(page.map(AdminOutboxResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminOutboxDetailResponse findById(Long id) {
        OutboxEvent event = outboxRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.OUTBOX_EVENT_NOT_FOUND));

        return AdminOutboxDetailResponse.from(event);
    }

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

        validateRetryable(event);

        return event;
    }

    private void validateRetryable(OutboxEvent event) {
        switch (event.getStatus()) {
            case FAILED, DEAD -> {
                return;
            }
            case SUCCESS -> throw new CustomException(ErrorCode.OUTBOX_ALREADY_SUCCEEDED);
            case PENDING -> throw new CustomException(ErrorCode.OUTBOX_ALREADY_PENDING);
            case PROCESSING -> throw new CustomException(ErrorCode.OUTBOX_ALREADY_PROCESSING);
            default -> throw new CustomException(ErrorCode.OUTBOX_RETRY_NOT_ALLOWED);
        }
    }
}
