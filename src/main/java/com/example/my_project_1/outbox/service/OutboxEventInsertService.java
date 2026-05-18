package com.example.my_project_1.outbox.service;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventInsertService {

    private final OutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutboxEvent saveAndFlush(OutboxEvent event) {
        return outboxRepository.saveAndFlush(event);
    }
}
