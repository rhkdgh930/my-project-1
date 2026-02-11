package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.auth.utils.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService redisUserContextService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getUserId();

        CachedUserContext ctx = redisUserContextService.getUserContext(userId);
        redisUserContextService.validateActiveUser(ctx);

        // 1. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(userId, userDetails.getRole());
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // 2. Redis 저장 (Refresh Token & User Context)
        redisTokenService.saveRefreshTokenHash(userId, refreshToken, jwtProvider.getRemainingValidityMillis(refreshToken));

        int refreshTokenMaxAge = (int) (jwtProvider.getRemainingValidityMillis(refreshToken) / 1000);
        int accessTokenMaxAge = 60 * 60; // 1시간 (혹은 토큰 만료시간에 맞춤)

        CookieUtils.addCookie(response, "accessToken", accessToken, accessTokenMaxAge);
        CookieUtils.addCookie(response, "refreshToken", refreshToken, refreshTokenMaxAge);


        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/api/auth/test") // 프론트 대신 백엔드로
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
