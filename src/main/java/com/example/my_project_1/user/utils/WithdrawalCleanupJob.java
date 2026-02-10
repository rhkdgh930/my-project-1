package com.example.my_project_1.user.utils;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawalCleanupJob {

    private final UserRepository userRepository;
    private final RedisUserContextService redisUserContextService;

    @Transactional
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void cleanupWithdrawnUsers() {

        LocalDateTime now = LocalDateTime.now();

        List<User> targets =
                userRepository.findAllByUserStatus(UserStatus.WITHDRAWN_REQUESTED);

        for (User user : targets) {
            if (user.getWithdrawal().shouldDelete(now)) {
                user.completeWithdrawal();
                redisUserContextService.evict(user.getId());
                log.info("[WithdrawalCleanup] userId={}", user.getId());
            }
        }
    }
}
