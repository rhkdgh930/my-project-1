package com.example.my_project_1.outbox.handler.impl;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.user.event.UserAccountChangedOutboxEvent;
import com.example.my_project_1.user.event.UserAccountChangedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserAccountChangedHandlerTest {

    private final RedisUserContextService redisUserContextService = mock(RedisUserContextService.class);
    private final RedisTokenService redisTokenService = mock(RedisTokenService.class);
    private final UserAccountChangedHandler handler =
            new UserAccountChangedHandler(redisUserContextService, redisTokenService);

    @Test
    @DisplayName("사용자 컨텍스트 캐시 evict 실패 시 예외를 전파한다.")
    void handle_fail_when_user_context_evict_fails() {
        Long userId = 1L;
        RuntimeException redisException = new RuntimeException("redis evict failed");
        doThrow(redisException).when(redisUserContextService).evict(userId);

        String payload = DataSerializer.serialize(
                new UserAccountChangedOutboxEvent(userId, UserAccountChangedType.SECURITY_CHANGED)
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isSameAs(redisException);
    }

    @Test
    @DisplayName("refresh token 삭제 실패 시 예외를 전파한다.")
    void handle_fail_when_refresh_token_delete_fails() {
        Long userId = 1L;
        RuntimeException redisException = new RuntimeException("redis token delete failed");
        doThrow(redisException).when(redisTokenService).deleteRefreshTokenHash(userId);

        String payload = DataSerializer.serialize(
                new UserAccountChangedOutboxEvent(userId, UserAccountChangedType.SECURITY_CHANGED)
        );

        assertThatThrownBy(() -> handler.handle(payload))
                .isSameAs(redisException);
    }

    @Test
    @DisplayName("캐시 evict와 refresh token 삭제가 필요한 이벤트를 처리한다.")
    void handle_success_when_cache_and_token_invalidation_succeed() {
        Long userId = 1L;
        String payload = DataSerializer.serialize(
                new UserAccountChangedOutboxEvent(userId, UserAccountChangedType.SECURITY_CHANGED)
        );

        handler.handle(payload);

        verify(redisUserContextService).evict(userId);
        verify(redisTokenService).deleteRefreshTokenHash(userId);
    }
}
