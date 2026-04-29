package com.example.my_project_1.auth.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisPasswordResetTokenServiceTest {

    private static final String RAW_TOKEN = "raw-reset-token";
    private static final String EMAIL = "email@email.com";

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final RedisPasswordResetTokenService service =
            new RedisPasswordResetTokenService(redisTemplate);

    @Test
    @DisplayName("consumeToken uses GETDEL on the hashed key and returns email")
    void consumeToken_getsAndDeletesTokenAtomically() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(key())).thenReturn(EMAIL);

        String email = service.consumeToken(RAW_TOKEN);

        assertThat(email).isEqualTo(EMAIL);
        verify(valueOperations).getAndDelete(key());
    }

    @Test
    @DisplayName("consumeToken throws INVALID_EMAIL_TOKEN when token is missing")
    void consumeToken_throwsWhenTokenIsMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(key())).thenReturn(null);

        assertThatThrownBy(() -> service.consumeToken(RAW_TOKEN))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EMAIL_TOKEN);
    }

    private String key() {
        return "auth::pw_reset::%s".formatted(DigestUtils.sha256Hex(RAW_TOKEN));
    }
}
