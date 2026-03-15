package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.utils.DataSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTokenService {
    private final StringRedisTemplate redisTemplate;

    private static final String RT_KEY = "auth::rt::%s";
    private static final String BL_KEY = "auth::bl::%s";
    private static final String HISTORY_KEY = "auth::history::%s";

    public void saveRefreshTokenHash(Long userId, String refreshToken, long ttl) {
        redisTemplate.opsForValue().set(
                RT_KEY.formatted(userId),
                hash(refreshToken),
                Duration.ofMillis(ttl)
        );
    }

    public String getRefreshTokenHash(Long userId) {
        return redisTemplate.opsForValue().get(RT_KEY.formatted(userId));
    }

    public void deleteRefreshTokenHash(Long userId) {
        redisTemplate.delete(RT_KEY.formatted(userId));
    }

    public void blacklistAccessToken(String accessToken, long ttl) {
        if (ttl <= 0) return;
        redisTemplate.opsForValue().set(
                BL_KEY.formatted(hash(accessToken)),
                "1",
                Duration.ofMillis(ttl)
        );
        log.debug(
                "[SECURITY][RedisTokenService][TOKEN_BLACKLISTED] ttlMs={}",
                ttl
        );
    }

    public boolean isBlacklisted(String accessToken) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(BL_KEY.formatted(hash(accessToken)))
            );
        } catch (Exception e) {
            log.error(
                    "[CACHE][RedisTokenService][REDIS_ACCESS_FAIL] operation=isBlacklisted",
                    e
            );
            throw new JwtAuthenticationException(ErrorCode.AUTHENTICATION_FAILED);
        }
    }

    public void saveReissueHistory(String oldRtHash, TokenResponse newTokenResponse) {
        String value = DataSerializer.serialize(newTokenResponse);
        redisTemplate.opsForValue().set(
                HISTORY_KEY.formatted(oldRtHash),
                value,
                Duration.ofSeconds(10)
        );
    }

    public TokenResponse getReissueHistory(String oldRtHash) {
        String value = redisTemplate.opsForValue().get(HISTORY_KEY.formatted(oldRtHash));
        if (value == null) return null;
        return DataSerializer
                .tryDeserialize(value, TokenResponse.class)
                .orElse(null);

    }

    public String getHash(String token) {
        return hash(token);
    }

    private String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
