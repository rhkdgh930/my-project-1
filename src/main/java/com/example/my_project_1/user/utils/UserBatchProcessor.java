package com.example.my_project_1.user.utils;

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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * [Chunk Transaction]
     * 상위 Job에는 트랜잭션이 없으며, 이 메서드 단위로 트랜잭션이 생성되고 종료(Commit)됩니다.
     * 장점: DB Connection 점유 시간을 최소화하고, 부분 실패를 허용합니다.
     */
    @Transactional
    public void processChunk(List<Long> userIds, LocalDateTime dormantThreshold) {
        // [Optimized Fetch] ID 리스트로 실제 엔티티 조회 (IN Query 사용)
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            // 조회 시점의 상태를 기준으로 판단 (Snapshot)
            if (user.getLastLoginAt().isBefore(dormantThreshold)) {
                // 1. 휴면 전환 (Dirty Checking)
                user.markDormant();
                redisUserContextService.evict(user.getId());
                log.debug("[Dormant] User {} marked as dormant.", user.getId());
            } else {
                // 2. 휴면 경고 알림 (Event 발행)
                // TransactionalEventListener에 의해 커밋 후에만 실제 메일이 발송됨
                eventPublisher.publishEvent(new DormancyNotifyEvent(
                        user.getId(),
                        user.getEmail().getValue(),
                        user.getNickname()
                ));
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
