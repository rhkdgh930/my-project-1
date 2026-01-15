package com.example.my_project_1.auth.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RedisEmailVerificationService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final String VERIFICATION_KEY = "auth::email::v::%s";
    private static final long LIMIT_TIME = 5 * 60; // 5분

    public void sendCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(key(email), code, Duration.ofSeconds(LIMIT_TIME));
        emailService.sendVerificationCode(email, code);
    }

    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(key(email));

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN); // 혹은 EXPIRED_VERIFICATION_CODE 등
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE); // 인증 코드 불일치
        }

        redisTemplate.delete(key(email));
    }

    private String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String key(String email) {
        return VERIFICATION_KEY.formatted(email);
    }
}
