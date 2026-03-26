package com.example.my_project_1.outbox.controller;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/outbox")
public class AdminOutboxController {

    private final Clock clock;
    private final OutboxRepository outboxRepository;

    @Transactional
    @PostMapping("/{id}/retry")
    public void retry(@PathVariable Long id) {
        OutboxEvent event = outboxRepository.findById(id)
                .orElseThrow();

        event.resetForRetry(LocalDateTime.now(clock));
    }
}
