package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
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
        redisUserContextService.validate(ctx);

        // 1. JWT 토큰 생성
        String accessToken = jwtProvider.createAccessToken(userId, userDetails.getRole());
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // 2. Redis 저장 (Refresh Token & User Context)
        redisTokenService.saveRefreshTokenHash(userId, refreshToken, jwtProvider.getRemainingValidityMillis(refreshToken));

        // 3. 프론트엔드로 리다이렉트 (Query Param으로 토큰 전달)
        // 주의: 실무에서는 RefreshToken을 HttpOnly Cookie에 담는 것을 권장합니다.
//        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/callback")
//                .queryParam("accessToken", accessToken)
//                .queryParam("refreshToken", refreshToken)
//                .build().toUriString();

        //백엔드에서 테스트하기 위한 임시경로
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/api/auth/test") // 프론트 대신 백엔드로
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
