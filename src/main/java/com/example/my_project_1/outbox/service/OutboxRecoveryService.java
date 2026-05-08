package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxRecoveryService {

    private static final int CHUNK_SIZE = 100;
    private static final int PROCESSING_TIMEOUT_MINUTES = 5;

    private final Clock clock;
    private final OutboxRepository outboxRepository;

    @Transactional
    public int recoverStuckEvents() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusMinutes(PROCESSING_TIMEOUT_MINUTES);

        List<Long> ids = outboxRepository.findStuckProcessingIds(
                threshold,
                PageRequest.ofSize(CHUNK_SIZE)
        );

        int recovered = 0;
        for (Long id : ids) {
            recovered += outboxRepository.findById(id)
                    .filter(event -> event.getStatus() == OutboxStatus.PROCESSING)
                    .map(event -> {
                        event.markProcessingTimeout(now);
                        return 1;
                    })
                    .orElse(0);
        }

        return recovered;
    }
}
