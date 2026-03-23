package com.example.my_project_1.user.batch;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.listener.OutboxMessageEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserBatchWorker {

    private final RedisUserContextService redisUserContextService;
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processSingleUserWithDormancy(User user, LocalDateTime threshold) {
        if (user.getLastLoginAt().isBefore(threshold)) {
            user.markDormant();
            redisUserContextService.evict(user.getId());
            return;
        }

        String payload = DataSerializer.serialize(
                new DormancyNotifyOutboxEvent(
                        user.getId(),
                        user.getEmail().getValue(),
                        user.getNickname()
                )
        );

        OutboxEvent outbox = outboxRepository.save(
                OutboxEvent.create(
                        OutboxEventType.DORMANCY_NOTIFY,
                        payload
                )
        );
        eventPublisher.publishEvent(new OutboxMessageEvent(outbox.getId()));
    }

    @Transactional
    public void processSingleWithdrawal(User user) {
        user.completeWithdrawal();
        redisUserContextService.evict(user.getId());
    }
}
