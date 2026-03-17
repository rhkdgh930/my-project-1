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

    public boolean tryMarkNotified(Long userId) {
        String key = key(userId);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(30));

        return Boolean.TRUE.equals(success);
    }

    public void deleteNotificationHistory(Long userId) {
        String key = key(userId);
        redisTemplate.delete(key);
    }

    private String key(Long userId) {
        return DORMANT_NOTIFY_KEY.formatted(userId);
    }
}
