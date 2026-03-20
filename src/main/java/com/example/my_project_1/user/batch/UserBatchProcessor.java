package com.example.my_project_1.user.batch;

import com.example.my_project_1.auth.service.RedisDormancyHistoryService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.DormancyNotifyEvent;
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
    private final RedisDormancyHistoryService redisDormancyHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processDormancyChunk(List<Long> userIds, LocalDateTime dormantThreshold) {
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {

            if (user.getLastLoginAt().isBefore(dormantThreshold)) {

                user.markDormant();

                redisUserContextService.evict(user.getId());

                log.info("[BATCH][Dormant] userId={}", user.getId());
                continue;
            }

            if (redisDormancyHistoryService.tryMarkNotified(user.getId())) {
                eventPublisher.publishEvent(
                        new DormancyNotifyEvent(
                                user.getId(),
                                user.getEmail().getValue(),
                                user.getNickname()
                        )
                );
                log.info("[BATCH][Notify] sent | userId={}", user.getId());

            } else {
                log.debug("[BATCH][Notify] skipped | userId={}", user.getId());
            }
        }
    }

    @Transactional
    public void processWithdrawalChunk(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {

            user.completeWithdrawal();

            redisUserContextService.evict(user.getId());

            log.info("[BATCH][Withdrawal] userId={}", user.getId());
        }
    }
}