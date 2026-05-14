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

    @Test
    @DisplayName("로그아웃 access token이 만료되면 blacklist를 생략하고 성공한다.")
    void logout_skipsBlacklistWhenAccessTokenExpired() {
        String accessToken = "expired-access-token";

        when(jwtProvider.parseClaimsSafely(accessToken))
                .thenThrow(new JwtAuthenticationException(ErrorCode.EXPIRED_ACCESS_TOKEN));

        authService.logout(accessToken, null);

        verify(redisTokenService, never()).blacklistAccessToken(anyString(), anyLong());
        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("로그아웃 access token이 유효하면 남은 TTL만큼 blacklist 처리한다.")
    void logout_blacklistsValidAccessToken() {
        String accessToken = "access-token";
        Claims claims = mock(Claims.class);

        when(jwtProvider.parseClaimsSafely(accessToken)).thenReturn(claims);
        when(jwtProvider.getRemainingValidityMillis(claims)).thenReturn(60_000L);

        authService.logout(accessToken, null);

        verify(jwtProvider).assertAccessToken(claims);
        verify(redisTokenService).blacklistAccessToken(accessToken, 60_000L);
    }

    @Test
    @DisplayName("로그아웃 access token이 없으면 blacklist를 생략하고 성공한다.")
    void logout_skipsBlacklistWhenAccessTokenMissing() {
        authService.logout(null, null);

        verifyNoInteractions(jwtProvider);
        verify(redisTokenService, never()).blacklistAccessToken(anyString(), anyLong());
        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("로그아웃 refresh token이 있으면 userId를 읽고 refresh token hash를 삭제한다.")
    void logout_deletesRefreshTokenHashWhenRefreshTokenPresent() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Claims claims = mock(Claims.class);

        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(redisTokenService.getRefreshTokenHash(1L)).thenReturn(requestHash);

        authService.logout(null, refreshToken);

        verify(jwtProvider).assertRefreshToken(claims);
        verify(redisTokenService).getRefreshTokenHash(1L);
        verify(redisTokenService).getHash(refreshToken);
        verify(redisTokenService).deleteRefreshTokenHash(1L);
        verify(redisTokenService, never()).blacklistAccessToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("로그아웃 access token이 만료되어도 refresh token 정리는 계속 진행한다.")
    void logout_deletesRefreshTokenHashWhenAccessTokenExpired() {
        String accessToken = "expired-access-token";
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Claims refreshClaims = mock(Claims.class);

        when(jwtProvider.parseClaimsSafely(accessToken))
                .thenThrow(new JwtAuthenticationException(ErrorCode.EXPIRED_ACCESS_TOKEN));
        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(refreshClaims);
        when(refreshClaims.getSubject()).thenReturn("1");
        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(redisTokenService.getRefreshTokenHash(1L)).thenReturn(requestHash);

        authService.logout(accessToken, refreshToken);

        verify(redisTokenService, never()).blacklistAccessToken(anyString(), anyLong());
        verify(jwtProvider).assertRefreshToken(refreshClaims);
        verify(redisTokenService).deleteRefreshTokenHash(1L);
    }

    @Test
    @DisplayName("로그아웃 refresh token의 저장된 hash가 없으면 INVALID_REFRESH_TOKEN을 전파한다.")
    void logout_rejectsRefreshTokenWhenStoredHashIsMissing() {
        String refreshToken = "refresh-token";
        String requestHash = "request-hash";
        Claims claims = mock(Claims.class);

        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(redisTokenService.getHash(refreshToken)).thenReturn(requestHash);
        when(redisTokenService.getRefreshTokenHash(1L)).thenReturn(null);

        assertThatThrownBy(() -> authService.logout(null, refreshToken))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("로그아웃 refresh token hash가 Redis current hash와 다르면 INVALID_REFRESH_TOKEN을 전파한다.")
    void logout_rejectsRefreshTokenWhenStoredHashDoesNotMatch() {
        String refreshToken = "refresh-token";
        Claims claims = mock(Claims.class);

        when(jwtProvider.parseClaimsSafely(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(redisTokenService.getHash(refreshToken)).thenReturn("request-hash");
        when(redisTokenService.getRefreshTokenHash(1L)).thenReturn("stored-hash");

        assertThatThrownBy(() -> authService.logout(null, refreshToken))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
    }

    @Test
    @DisplayName("로그아웃 refresh token이 유효하지 않으면 INVALID_REFRESH_TOKEN을 전파한다.")
    void logout_rejectsInvalidRefreshToken() {
        String refreshToken = "invalid-refresh-token";
        JwtAuthenticationException exception = new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);

        when(jwtProvider.parseClaimsSafely(refreshToken)).thenThrow(exception);

        assertThatThrownBy(() -> authService.logout(null, refreshToken))
                .isSameAs(exception);

        verify(redisTokenService, never()).deleteRefreshTokenHash(anyLong());
        verify(redisTokenService, never()).blacklistAccessToken(anyString(), anyLong());
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
