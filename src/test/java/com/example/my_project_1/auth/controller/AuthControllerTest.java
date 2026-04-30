package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final AuthController authController = new AuthController(authService, jwtProvider);

    @Test
    @DisplayName("토큰 재발급 시 쿠키에만 RefreshToken이 있으면 쿠키 값을 사용한다.")
    void reissue_usesCookieRefreshToken() {
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.reissue("cookie-refresh-token")).thenReturn(tokenResponse);
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(60_000L);

        ResponseEntity<TokenResponse> result =
                authController.reissue("cookie-refresh-token", null, response);

        assertThat(result.getBody()).isSameAs(tokenResponse);
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=new-refresh-token");
        verify(authService).reissue("cookie-refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 시 헤더에만 RefreshToken이 있으면 헤더 값을 사용한다.")
    void reissue_usesHeaderRefreshToken() {
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.reissue("header-refresh-token")).thenReturn(tokenResponse);
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(60_000L);

        ResponseEntity<TokenResponse> result =
                authController.reissue(null, "header-refresh-token", response);

        assertThat(result.getBody()).isSameAs(tokenResponse);
        verify(authService).reissue("header-refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 시 쿠키와 헤더의 RefreshToken이 같으면 요청을 허용한다.")
    void reissue_acceptsMatchingCookieAndHeaderRefreshToken() {
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.reissue("same-refresh-token")).thenReturn(tokenResponse);
        when(jwtProvider.getRemainingValidityMillis("new-refresh-token")).thenReturn(60_000L);

        ResponseEntity<TokenResponse> result =
                authController.reissue("same-refresh-token", "same-refresh-token", response);

        assertThat(result.getBody()).isSameAs(tokenResponse);
        verify(authService).reissue("same-refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 시 쿠키와 헤더의 RefreshToken이 다르면 요청을 거부한다.")
    void reissue_rejectsConflictingCookieAndHeaderRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() ->
                authController.reissue("cookie-refresh-token", "header-refresh-token", response)
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verifyNoInteractions(authService);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("토큰 재발급 시 RefreshToken이 없으면 요청을 거부한다.")
    void reissue_rejectsMissingRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authController.reissue(null, null, response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verifyNoInteractions(authService);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("로그아웃 시 쿠키와 헤더의 RefreshToken이 같으면 요청을 허용한다.")
    void logout_acceptsMatchingCookieAndHeaderRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.logout(
                "Bearer access-token",
                "same-refresh-token",
                "same-refresh-token",
                response
        );

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=");
        verify(authService).logout("access-token", "same-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 시 쿠키와 헤더의 RefreshToken이 다르면 요청을 거부한다.")
    void logout_rejectsConflictingCookieAndHeaderRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authController.logout(
                "Bearer access-token",
                "cookie-refresh-token",
                "header-refresh-token",
                response
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        verifyNoInteractions(authService);
    }
}