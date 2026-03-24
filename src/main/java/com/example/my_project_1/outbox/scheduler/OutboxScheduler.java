package com.example.my_project_1.outbox.scheduler;

import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.outbox.service.OutboxProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private static final int CHUNK_SIZE = 100;
    private final OutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelay = 5_000)
    public void run() {
        List<Long> ids =
                outboxRepository.findProcessableIds(
                        LocalDateTime.now(),
                        PageRequest.ofSize(CHUNK_SIZE)
                );
        ids.forEach(outboxProcessor::process);
    }
}
