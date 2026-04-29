package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.service.UserLoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2LoginSuccessHandlerTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RedisTokenService redisTokenService = mock(RedisTokenService.class);
    private final UserLoginService userLoginService = mock(UserLoginService.class);
    private final OAuth2LoginSuccessHandler handler =
            new OAuth2LoginSuccessHandler(jwtProvider, redisTokenService, userLoginService);

    @Test
    @DisplayName("OAuth2 로그인 성공 redirect URL에 access token을 노출하지 않는다.")
    void oauth2SuccessRedirectDoesNotExposeAccessTokenInQueryParam() throws Exception {
        Long userId = 1L;
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        Authentication authentication = getAuthentication(userId);

        when(jwtProvider.createAccessToken(userId, "USER")).thenReturn(accessToken);
        when(jwtProvider.createRefreshToken(userId)).thenReturn(refreshToken);
        when(jwtProvider.getRemainingValidityMillis(refreshToken)).thenReturn(3_600_000L);

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).doesNotContain("accessToken");
        assertThat(response.getRedirectedUrl()).doesNotContain(accessToken);
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).anyMatch(cookie -> cookie.contains("accessToken=" + accessToken));
        assertThat(cookies).anyMatch(cookie -> cookie.contains("refreshToken=" + refreshToken));
        verify(userLoginService).processLogin(userId);
        verify(redisTokenService).saveRefreshTokenHash(userId, refreshToken, 3_600_000L);
    }

    private static Authentication getAuthentication(Long userId) {
        UserDetailsImpl principal = new UserDetailsImpl(
                userId,
                "email@email.com",
                null,
                "USER",
                AccountStatus.NORMAL,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                null,
                false,
                false
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        return authentication;
    }
}
