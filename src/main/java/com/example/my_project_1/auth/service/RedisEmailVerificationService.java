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

    private static final String VERIFICATION_KEY = "auth::email::verify::%s";
    private static final String VERIFIED_FLAG_KEY = "auth::email::success::%s";
    private static final long CODE_LIMIT_TIME = 5 * 60; // 인증번호 유효시간 5분
    private static final long VERIFIED_LIMIT_TIME = 30 * 60; // 인증 완료 상태 유효시간 30분

    public void sendCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(key(email), code, Duration.ofSeconds(CODE_LIMIT_TIME));
        emailService.sendVerificationCode(email, code);
    }

    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(key(email));

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.WRONG_VERIFICATION_CODE);
        }

        redisTemplate.opsForValue().set(verifiedKey(email), "true", Duration.ofSeconds(VERIFIED_LIMIT_TIME));

        redisTemplate.delete(key(email));
    }

    public void checkIsVerified(String email) {
        String isVerified = redisTemplate.opsForValue().get(verifiedKey(email));
        if (isVerified == null) {
            throw new CustomException(ErrorCode.UNVERIFIED_EMAIL);
        }
    }

    public void deleteVerifiedStatus(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    private String generateCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String key(String email) {
        return VERIFICATION_KEY.formatted(email);
    }

    private String verifiedKey(String email) {
        return VERIFIED_FLAG_KEY.formatted(email);
    }
}