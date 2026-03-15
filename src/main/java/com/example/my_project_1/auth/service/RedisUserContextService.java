package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisUserContextService {

    private final Clock clock;

    private final RedisTemplate<String, CachedUserContext> userContextRedisTemplate;
    private final UserRepository userRepository;

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String USER_CTX_KEY = "auth::user::ctx::%s";

    public CachedUserContext getUserContext(Long userId) {
        String key = key(userId);

        try {
            CachedUserContext cached = userContextRedisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info(
                        "[CACHE][UserContext][HIT] userId={}",
                        userId
                );
                return cached;
            }
            log.info("[CACHE][UserContext][MISS] userId={}", userId);

        } catch (Exception e) {
            log.error(
                    "[CACHE][UserContext][GET_FAIL] userId={} errorType={} message={}",
                    userId,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        CachedUserContext ctx = CachedUserContext.from(user, LocalDateTime.now(clock));

        try {
            userContextRedisTemplate.opsForValue().set(key, ctx, TTL);
        } catch (Exception e) {
            log.warn(
                    "[CACHE][UserContext][PUT_FAIL] userId={}",
                    userId,
                    e
            );
        }
        return ctx;
    }

    public void validateActiveUser(CachedUserContext ctx) {
        if (ctx.isDeleted()) {
            throw new JwtAuthenticationException(ErrorCode.USER_NOT_FOUND);
        }

        if (ctx.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new JwtAuthenticationException(ErrorCode.USER_SUSPENDED);
        }
    }

    public void evict(Long userId) {
        userContextRedisTemplate.delete(key(userId));
        log.debug(
                "[CACHE][UserContext][EVICT] userId={}",
                userId
        );
    }

    private String key(Long userId) {
        return USER_CTX_KEY.formatted(userId);
    }
}