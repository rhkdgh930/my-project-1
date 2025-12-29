package com.example.my_project_1.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(String email, String token, long ttl) {
        redisTemplate.opsForValue().set(rtKey(email), token, Duration.ofMillis(ttl));
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(rtKey(email));
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(rtKey(email));
    }

    public void blacklistAccessToken(String token, long ttl) {
        if (ttl <= 0) return;
        redisTemplate.opsForValue().set(blKey(token), "1", Duration.ofMillis(ttl));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blKey(token)));
    }

    private String rtKey(String email) {
        return "RT:" + email;
    }

    private String blKey(String token) {
        return "BL:" + token;
    }
}
