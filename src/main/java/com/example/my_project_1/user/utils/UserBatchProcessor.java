package com.example.my_project_1.user.utils;

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

    /**
     * [Chunk Transaction]
     * 상위 Job에는 트랜잭션이 없으며, 이 메서드 단위로 트랜잭션이 생성되고 종료(Commit)됩니다.
     * 장점: DB Connection 점유 시간을 최소화하고, 부분 실패를 허용합니다.
     */
    @Transactional
    public void processDormancyChunk(List<Long> userIds, LocalDateTime dormantThreshold) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            // 1. 휴면 전환 로직 (12개월 경과)
            if (user.getLastLoginAt().isBefore(dormantThreshold)) {
                user.markDormant();
                redisUserContextService.evict(user.getId());
                log.debug("[Dormant] User {} marked as dormant.", user.getId());
                continue;
            }

            // 2. 휴면 경고 알림 로직 (11개월 경과)
            // 💡 Redis를 조회하여 이번 달에 보낸 적이 있는지 확인
            if (!redisDormancyHistoryService.hasBeenNotified(user.getId())) {
                eventPublisher.publishEvent(new DormancyNotifyEvent(
                        user.getId(),
                        user.getEmail().getValue(),
                        user.getNickname()
                ));

                // 💡 이벤트 발행 후 Redis에 기록 저장 (30일 TTL)
                redisDormancyHistoryService.setNotificationHistory(user.getId());
                log.info("[Dormant-Batch] Alert sent to userId={}", user.getId());
            } else {
                log.debug("[Dormant-Batch] Skip alert for userId={} (Already notified)", user.getId());
            }
        }
    }

    @Transactional
    public void processWithdrawalChunk(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            user.completeWithdrawal();
            redisUserContextService.evict(user.getId());
            log.info("[WithdrawalCleanup] userId={} completely withdrawn.", user.getId());
        }
    }
}
