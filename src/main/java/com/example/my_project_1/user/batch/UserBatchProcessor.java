package com.example.my_project_1.user.batch;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.listener.OutboxMessageEvent;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.DormancyNotifyOutboxEvent;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchProcessor {

    private final UserRepository userRepository;
    private final RedisUserContextService redisUserContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxRepository outboxRepository;

    public void processDormancyChunk(List<Long> userIds, LocalDateTime dormantThreshold) {
        for (Long userId : userIds) {
            try {
                processSingleUserWithDormancy(userId, dormantThreshold);
            } catch (Exception e) {
                log.error("[BATCH][Dormancy][USER_FAIL] userId={}", userId, e);
            }
        }
    }

    @Transactional
    public void processSingleUserWithDormancy(Long userId, LocalDateTime dormantThreshold) {
        User user = userRepository.findById(userId)
                .orElseThrow();

        if (user.getLastLoginAt().isBefore(dormantThreshold)) {

            user.markDormant();
            redisUserContextService.evict(userId);

            log.info("[BATCH][Dormant] userId={}", userId);
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

        eventPublisher.publishEvent(
                new OutboxMessageEvent(outbox.getId())
        );

        log.info("[BATCH][Notify] queued | userId={}", userId);
    }

    public void processWithdrawalChunk(List<Long> userIds) {
        for (Long userId : userIds) {
            try {
                processSingleWithdrawal(userId);
            } catch (Exception e) {
                log.error("[BATCH][Withdrawal][USER_FAIL] userId={}", userId, e);
            }
        }
    }

    @Transactional
    public void processSingleWithdrawal(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();

        user.completeWithdrawal();

        redisUserContextService.evict(userId);

        log.info("[BATCH][Withdrawal] userId={}", userId);
    }
}