package com.example.my_project_1.user.event.listener;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.event.UserAccountChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAccountChangedEventListener {
    private final RedisUserContextService redisUserContextService;
    private final RedisTokenService redisTokenService;

    @Async("asyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAccountChanged(UserAccountChangedEvent event) {

        Long userId = event.getUserId();
        log.info(
                "[SECURITY][UserAccountChanged] tokens revoked | userId={}",
                userId
        );
        try {
            redisUserContextService.evict(userId);

            if (event.isSecurityCritical()) {
                redisTokenService.deleteRefreshTokenHash(userId);
            }

        } catch (Exception e) {
            log.error(
                    "[CACHE][UserAccountChanged] cache eviction failed | userId={}",
                    event.getUserId(),
                    e
            );
        }
    }
}
