package com.example.my_project_1.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RedisLoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final String LOGIN_ATTEMPTS_KEY = "auth::login::attempts::%s";
    private static final Duration LOCK_TIME = Duration.ofMinutes(10);

    public void loginFailed(String email) {
        String normalizedEmail = normalize(email);

        if (!StringUtils.hasText(normalizedEmail)) {
            return;
        }

        String key = key(normalizedEmail);

        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, LOCK_TIME);
        }
    }

    public boolean isBlocked(String email) {
        String normalizedEmail = normalize(email);

        if (!StringUtils.hasText(normalizedEmail)) {
            return false;
        }

        String attempts = redisTemplate.opsForValue().get(key(normalizedEmail));

        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    public void loginSucceeded(String email) {
        String normalizedEmail = normalize(email);

        if (StringUtils.hasText(normalizedEmail)) {
            redisTemplate.delete(key(normalizedEmail));
        }
    }

    private String key(String email) {
        return LOGIN_ATTEMPTS_KEY.formatted(email);
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}