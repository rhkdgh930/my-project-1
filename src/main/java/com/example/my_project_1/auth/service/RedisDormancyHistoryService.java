package com.example.my_project_1.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RedisDormancyHistoryService {
    private final StringRedisTemplate redisTemplate;
    private static final String DORMANT_NOTIFY_KEY = "user::dormant::notified::%s";

    public boolean executeOnce(Long userId, Duration ttl, Runnable action) {
        String key = key(userId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttl);

        if (!Boolean.TRUE.equals(success)) {
            return false;
        }

        try {
            action.run();
            return true;
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw e;
        }
    }

    private String key(Long userId) {
        return DORMANT_NOTIFY_KEY.formatted(userId);
    }
}
