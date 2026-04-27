package com.example.my_project_1.auth.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisPasswordResetTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String RESET_KEY = "auth::pw_reset::%s";
    private static final long EXPIRE_SECONDS = 5 * 60;

    public void saveToken(String rawToken, String email) {
        redisTemplate.opsForValue().set(
                key(hash(rawToken)),
                email,
                Duration.ofSeconds(EXPIRE_SECONDS)
        );
    }

    public String validateAndGetEmail(String rawToken) {
        String hashedToken = hash(rawToken);
        String email = redisTemplate.opsForValue().get(key(hashedToken));

        if (email == null) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_TOKEN);
        }

        return email;
    }

    public void deleteToken(String rawToken) {
        String hashedToken = hash(rawToken);
        redisTemplate.delete(key(hashedToken));
    }

    private String key(String hashedToken) {
        return RESET_KEY.formatted(hashedToken);
    }

    private String hash(String rawToken) {
        return DigestUtils.sha256Hex(rawToken);
    }
}