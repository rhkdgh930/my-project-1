package com.example.my_project_1.auth.service.impl;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.Role;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    private final Clock clock = Clock.systemDefaultZone();
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RedisTokenService redisTokenService = mock(RedisTokenService.class);
    private final RedisUserContextService userContextService = mock(RedisUserContextService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher =
            mock(UserAccountChangeOutboxPublisher.class);
    private final AuthServiceImpl authService = new AuthServiceImpl(
            clock,
            jwtProvider,
            passwordEncoder,
            redisTokenService,
            userContextService,
            userRepository,
            userAccountChangeOutboxPublisher
    );

    @Test
    @DisplayName("refresh token 재발급은 사용자 활성 상태를 검증한다.")
    void reissue_validates_active_user_context() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        CachedUserContext ctx = activeContext(userId);

        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(userContextService.getUserContext(userId)).thenReturn(ctx);
        when(jwtProvider.createAccessToken(userId, Role.USER.name())).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(1000L);
        when(redisTokenService.rotateRefreshToken(
                eq(userId),
                eq(requestHash),
                eq("new-refresh-token"),
                eq(1000L),
                any(TokenResponse.class)
        )).thenReturn(true);

        authService.reissue(refreshToken);

        verify(userContextService).validateActiveUser(ctx);
    }

    @Test
    @DisplayName("refresh token 재발급은 Redis 원자 rotation이 성공하면 새 토큰을 반환한다.")
    void reissue_returnsNewTokenWhenRotationSucceeds() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        CachedUserContext ctx = activeContext(userId);

        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(userContextService.getUserContext(userId)).thenReturn(ctx);
        when(jwtProvider.createAccessToken(userId, Role.USER.name())).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(1000L);
        when(redisTokenService.rotateRefreshToken(
                eq(userId),
                eq(requestHash),
                eq("new-refresh-token"),
                eq(1000L),
                any(TokenResponse.class)
        )).thenReturn(true);

        TokenResponse response = authService.reissue(refreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(redisTokenService).rotateRefreshToken(
                eq(userId),
                eq(requestHash),
                eq("new-refresh-token"),
                eq(1000L),
                any(TokenResponse.class)
        );
        verify(redisTokenService, never()).saveRefreshTokenHash(anyLong(), anyString(), anyLong());
        verify(redisTokenService, never()).saveReissueHistory(anyString(), any(TokenResponse.class));
        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("rotation이 실패해도 reissue history가 있으면 cached token을 반환한다.")
    void reissue_returnsCachedTokenWhenRotationFailsWithHistory() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        CachedUserContext ctx = activeContext(userId);
        TokenResponse cachedResponse = new TokenResponse("cached-access-token", "cached-refresh-token");

        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(userContextService.getUserContext(userId)).thenReturn(ctx);
        when(jwtProvider.createAccessToken(userId, Role.USER.name())).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(1000L);
        when(redisTokenService.rotateRefreshToken(
                eq(userId),
                eq(requestHash),
                eq("new-refresh-token"),
                eq(1000L),
                any(TokenResponse.class)
        )).thenReturn(false);
        when(redisTokenService.getReissueHistory(requestHash))
                .thenReturn(null)
                .thenReturn(cachedResponse);

        TokenResponse response = authService.reissue(refreshToken);

        assertThat(response).isSameAs(cachedResponse);
        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("rotation 실패 후 history도 없으면 refresh token state를 삭제하고 INVALID_REFRESH_TOKEN을 던진다.")
    void reissue_deletesRefreshTokenHashWhenRotationFailsWithoutHistory() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        CachedUserContext ctx = activeContext(userId);

        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(userContextService.getUserContext(userId)).thenReturn(ctx);
        when(jwtProvider.createAccessToken(userId, Role.USER.name())).thenReturn("new-access-token");
        when(jwtProvider.createRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(1000L);
        when(redisTokenService.rotateRefreshToken(
                eq(userId),
                eq(requestHash),
                eq("new-refresh-token"),
                eq(1000L),
                any(TokenResponse.class)
        )).thenReturn(false);

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(redisTokenService).deleteRefreshTokenHash(userId);
    }

    @Test
    @DisplayName("비활성 사용자면 cached reissue history가 있어도 재발급을 거부한다.")
    void reissue_rejects_inactive_user_before_cached_history() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Long userId = 1L;
        Claims claims = mock(Claims.class);
        CachedUserContext ctx = activeContext(userId);
        JwtAuthenticationException exception = new JwtAuthenticationException(ErrorCode.USER_SUSPENDED);

        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(userContextService.getUserContext(userId)).thenReturn(ctx);
        doThrow(exception).when(userContextService).validateActiveUser(ctx);
        when(redisTokenService.getReissueHistory(requestHash))
                .thenReturn(new TokenResponse("cached-access-token", "cached-refresh-token"));

        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isSameAs(exception);

        verify(redisTokenService, never()).rotateRefreshToken(
                anyLong(),
                anyString(),
                anyString(),
                anyLong(),
                any(TokenResponse.class)
        );
        verify(redisTokenService, never()).saveRefreshTokenHash(anyLong(), anyString(), anyLong());
        verify(jwtProvider, never()).createAccessToken(anyLong(), anyString());
        verify(jwtProvider, never()).createRefreshToken(anyLong());
    }

    private CachedUserContext activeContext(Long userId) {
        return new CachedUserContext(
                userId,
                "email@email.com",
                Role.USER,
                UserStatus.ACTIVE,
                AccountStatus.NORMAL,
                null,
                null,
                null,
                null,
                false,
                false
        );
    }
}
