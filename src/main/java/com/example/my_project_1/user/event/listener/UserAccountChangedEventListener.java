package com.example.my_project_1.user.event.listener;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
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
    public void handleUserAccountChanged(UserAccountChangedOutboxEvent event) {

        Long userId = event.getUserId();
        UserAccountChangedType type = event.getType();

        log.info(
                "[EVENT][UserAccountChangedEventListener] userId={} type={}",
                userId,
                type
        );

        try {
            if (type.shouldEvictCache()) {
                redisUserContextService.evict(userId);
            }

            if (type.shouldInvalidateToken()) {
                redisTokenService.deleteRefreshTokenHash(userId);
            }

        } catch (Exception e) {
            log.error(
                    "[CACHE][UserAccountChangedEventListener][FAIL] userId={}",
                    userId,
                    e
            );
        }
    }
}
