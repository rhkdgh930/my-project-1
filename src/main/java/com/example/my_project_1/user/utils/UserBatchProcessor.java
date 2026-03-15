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
            if (user.getLastLoginAt().isBefore(dormantThreshold)) {
                user.markDormant();
                redisUserContextService.evict(user.getId());
                log.debug(
                        "[BATCH][DormantUserJob] user marked dormant | userId={}",
                        user.getId()
                );
                continue;
            }

            if (!redisDormancyHistoryService.hasBeenNotified(user.getId())) {
                eventPublisher.publishEvent(new DormancyNotifyEvent(
                        user.getId(),
                        user.getEmail().getValue(),
                        user.getNickname()
                ));

                redisDormancyHistoryService.setNotificationHistory(user.getId());
                log.info(
                        "[BATCH][DormantUserJob] dormancy warning sent | userId={}",
                        user.getId()
                );
            } else {
                log.debug(
                        "[BATCH][DormantUserJob] skip dormancy warning (already notified) | userId={}",
                        user.getId()
                );
            }
        }
    }

    @Transactional
    public void processWithdrawalChunk(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            user.completeWithdrawal();
            redisUserContextService.evict(user.getId());
            log.info(
                    "[BATCH][WithdrawalCleanup] user withdrawn | userId={}",
                    user.getId()
            );
        }
    }
}
