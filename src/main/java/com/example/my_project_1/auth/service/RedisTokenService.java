package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.service.response.TokenResponse;
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

    private static final String RT_PREFIX = "RT:";
    private static final String BL_PREFIX = "BL:";
    private static final String HISTORY_PREFIX = "RT_HISTORY:";

    public void saveRefreshTokenHash(Long userId, String refreshToken, long ttl) {
        redisTemplate.opsForValue().set(
                RT_PREFIX + userId,
                hash(refreshToken),
                Duration.ofMillis(ttl)
        );
    }

    public String getRefreshTokenHash(Long userId) {
        return redisTemplate.opsForValue().get(RT_PREFIX + userId);
    }

    public void deleteRefreshTokenHash(Long userId) {
        redisTemplate.delete(RT_PREFIX + userId);
    }

    public void blacklistAccessToken(String accessToken, long ttl) {
        if (ttl <= 0) return;
        redisTemplate.opsForValue().set(
                BL_PREFIX + hash(accessToken),
                "1",
                Duration.ofMillis(ttl)
        );
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BL_PREFIX + hash(accessToken))
        );
    }

    public void saveReissueHistory(String oldRtHash, TokenResponse newTokenResponse) {
        String value = DataSerializer.serialize(newTokenResponse);
        redisTemplate.opsForValue().set(
                HISTORY_PREFIX + oldRtHash,
                value,
                Duration.ofSeconds(10) // 10초 동안만 기억
        );
    }

    public TokenResponse getReissueHistory(String oldRtHash) {
        String value = redisTemplate.opsForValue().get(HISTORY_PREFIX + oldRtHash);
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
