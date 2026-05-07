package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.service.response.TokenResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisTokenServiceTest {

    private static final Long USER_ID = 1L;
    private static final String OLD_REFRESH_TOKEN_HASH = "old-refresh-token-hash";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final long NEW_REFRESH_TOKEN_TTL = 1000L;

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RedisTokenService service = new RedisTokenService(redisTemplate);

    @Test
    @DisplayName("rotateRefreshToken은 Lua script로 refresh hash와 reissue history를 원자적으로 저장한다.")
    void rotateRefreshToken_executesLuaScriptWithHashedRefreshToken() {
        TokenResponse response = new TokenResponse("new-access-token", NEW_REFRESH_TOKEN);
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(1L);

        boolean rotated = service.rotateRefreshToken(
                USER_ID,
                OLD_REFRESH_TOKEN_HASH,
                NEW_REFRESH_TOKEN,
                NEW_REFRESH_TOKEN_TTL,
                response
        );

        assertThat(rotated).isTrue();
        verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("auth::rt::1", "auth::history::old-refresh-token-hash")),
                eq(OLD_REFRESH_TOKEN_HASH),
                eq(DigestUtils.sha256Hex(NEW_REFRESH_TOKEN)),
                eq(String.valueOf(NEW_REFRESH_TOKEN_TTL)),
                eq("10"),
                ArgumentMatchers.contains("\"accessToken\":\"new-access-token\"")
        );
    }

    @Test
    @DisplayName("rotateRefreshToken은 Lua script가 mismatch를 반환하면 false를 반환한다.")
    void rotateRefreshToken_returnsFalseWhenCurrentHashMismatches() {
        TokenResponse response = new TokenResponse("new-access-token", NEW_REFRESH_TOKEN);
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(0L);

        boolean rotated = service.rotateRefreshToken(
                USER_ID,
                OLD_REFRESH_TOKEN_HASH,
                NEW_REFRESH_TOKEN,
                NEW_REFRESH_TOKEN_TTL,
                response
        );

        assertThat(rotated).isFalse();
    }
}
