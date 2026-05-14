package com.example.my_project_1.auth.controller;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.service.AuthService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.utils.AuthTokenResolver;
import com.example.my_project_1.auth.utils.CookieManager;
import com.example.my_project_1.auth.utils.CookieProperties;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.GlobalExceptionHandler;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final CookieManager cookieManager = new CookieManager(cookieProperties());
    private final AuthTokenResolver authTokenResolver = new AuthTokenResolver();
    private final AuthController authController = new AuthController(authService, jwtProvider, cookieManager, authTokenResolver);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    private CookieProperties cookieProperties() {
        CookieProperties properties = new CookieProperties();
        properties.setRefreshTokenName("refreshToken");
        properties.setPath("/");
        properties.setHttpOnly(true);
        properties.setSecure(false);
        properties.setSameSite("Lax");
        return properties;
    }

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

    @Test
    @DisplayName("로그아웃 Authorization header가 없으면 INVALID_ACCESS_TOKEN으로 거절한다.")
    void logout_acceptsMissingAuthorizationHeaderAndRefreshToken() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.logout(
                null,
                null,
                null,
                response
        );

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=");
        verify(authService).logout(null, null);
    }

    @Test
    @DisplayName("로그아웃 Authorization header가 없어도 RefreshToken cookie가 있으면 서비스에 전달한다.")
    void logout_acceptsMissingAuthorizationHeaderWithRefreshTokenCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.logout(
                null,
                "cookie-refresh-token",
                null,
                response
        );

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=");
        verify(authService).logout(null, "cookie-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 Authorization header가 없어도 Refresh-Token header가 있으면 서비스에 전달한다.")
    void logout_acceptsMissingAuthorizationHeaderWithRefreshTokenHeader() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Void> result = authController.logout(
                null,
                null,
                "header-refresh-token",
                response
        );

        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=");
        verify(authService).logout(null, "header-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 Authorization header가 Bearer 형식이 아니면 INVALID_ACCESS_TOKEN으로 거절한다.")
    void logout_rejectsInvalidAuthorizationHeaderFormat() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authController.logout(
                "access-token",
                null,
                null,
                response
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ACCESS_TOKEN);

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그아웃 Authorization header가 없으면 HTTP 401 ExceptionResponse를 반환한다.")
    void logout_missingAuthorizationHeaderAndRefreshToken_returnsOkResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        verify(authService).logout(null, null);
    }

    @Test
    @DisplayName("로그아웃 Authorization header가 Bearer 형식이 아니면 HTTP 401 ExceptionResponse를 반환한다.")
    void logout_invalidAuthorizationHeaderFormat_returnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ACCESS_TOKEN.name()));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그아웃 RefreshToken cookie와 header가 같으면 HTTP 200으로 허용한다.")
    void logout_matchingCookieAndHeaderRefreshToken_returnsOkResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "same-refresh-token"))
                        .header("Refresh-Token", "same-refresh-token"))
                .andExpect(status().isOk());

        verify(authService).logout(null, "same-refresh-token");
    }

    @Test
    @DisplayName("로그아웃 RefreshToken cookie와 header가 다르면 INVALID_REFRESH_TOKEN을 반환한다.")
    void logout_conflictingCookieAndHeaderRefreshToken_returnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", "cookie-refresh-token"))
                        .header("Refresh-Token", "header-refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REFRESH_TOKEN.name()));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("로그아웃 유효하지 않은 RefreshToken이면 HTTP 401 ExceptionResponse를 반환한다.")
    void logout_invalidRefreshToken_returnsUnauthorizedResponse() throws Exception {
        doThrow(new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN))
                .when(authService).logout(null, "invalid-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Refresh-Token", "invalid-refresh-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REFRESH_TOKEN.name()));

        verify(authService).logout(null, "invalid-refresh-token");
    }
}
