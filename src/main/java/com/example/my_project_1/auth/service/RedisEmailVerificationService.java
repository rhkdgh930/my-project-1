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

    // 2. 인증번호 검증 (성공 시 '인증 증표' 저장)
    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(key(email));

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 인증 성공! -> Redis에 "이 이메일은 인증됨" 증표 저장
        redisTemplate.opsForValue().set(verifiedKey(email), "true", Duration.ofSeconds(VERIFIED_LIMIT_TIME));

        // 사용한 인증 코드는 삭제
        redisTemplate.delete(key(email));
    }

    // 3. (신규) 가입 시 인증 여부 확인
    public void checkIsVerified(String email) {
        String isVerified = redisTemplate.opsForValue().get(verifiedKey(email));
        if (isVerified == null) {
            throw new CustomException(ErrorCode.UNVERIFIED_EMAIL); // ErrorCode 추가 필요
        }
    }

    // 4. (신규) 인증 증표 사용 완료 처리 (가입 완료 후 삭제)
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