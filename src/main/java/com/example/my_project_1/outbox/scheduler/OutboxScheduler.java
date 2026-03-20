package com.example.my_project_1.outbox.scheduler;

import com.example.my_project_1.outbox.domain.OutboxEvent;
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
    private final OutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelay = 60000)
    public void run() {
        List<OutboxEvent> events =
                outboxRepository.findProcessableEvents(
                        LocalDateTime.now(),
                        PageRequest.of(0, 50)
                );
        events.forEach(e -> outboxProcessor.process(e.getId()));
    }
}
