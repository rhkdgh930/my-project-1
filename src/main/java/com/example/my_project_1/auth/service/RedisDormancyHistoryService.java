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

    public void setNotificationHistory(Long userId) {
        String key = key(userId);
        redisTemplate.opsForValue().set(key, "Y", Duration.ofDays(30));
    }

    public boolean hasBeenNotified(Long userId) {
        String key = key(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String key(Long userId) {
        return DORMANT_NOTIFY_KEY.formatted(userId);
    }
}
