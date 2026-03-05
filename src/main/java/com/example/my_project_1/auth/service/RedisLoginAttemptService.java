package com.example.my_project_1.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisLoginAttemptService {
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final String LOGIN_ATTEMPTS_KEY = "auth::login::attempts::%s";
    private static final Duration LOCK_TIME = Duration.ofMinutes(10); // 10분 차단

    public void loginFailed(String email) {
        String key = key(email);

        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, LOCK_TIME);
        }
    }

    public boolean isBlocked(String email) {
        String key = key(email);
        String attempts = redisTemplate.opsForValue().get(key);

        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    public void loginSucceeded(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return LOGIN_ATTEMPTS_KEY.formatted(email);
    }
}
