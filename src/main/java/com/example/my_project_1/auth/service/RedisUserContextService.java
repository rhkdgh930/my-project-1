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

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisUserContextService {

    private final RedisTemplate<String, CachedUserContext> redisTemplate;
    private final UserRepository userRepository;

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String KEY_FORMAT = "USER_CTX:";

    public CachedUserContext getUserContext(Long userId) {
        String key = key(userId);

        try {
            CachedUserContext cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.error("[RedisUserContextService.getUserContext] cache get : {}", e.getMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        CachedUserContext ctx = CachedUserContext.from(user);

        try {
            redisTemplate.opsForValue().set(key, ctx, TTL);
        } catch (Exception e) {
            log.error("[RedisUserContextService.getUserContext] cache put : {}", e.getMessage());
        }
        return ctx;
    }

    public void validate(CachedUserContext ctx) {
        if (ctx.isDeleted()) {
            throw new JwtAuthenticationException(ErrorCode.USER_NOT_FOUND);
        }

        if (ctx.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new JwtAuthenticationException(ErrorCode.USER_SUSPENDED);
        }
    }

    public void evict(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_FORMAT + userId;
    }
}