package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.CookieManager;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.user.service.UserLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final UserLoginService userLoginService;
    private final CookieManager cookieManager;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.oauth2.success-redirect-path}")
    private String successRedirectPath;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Long userId = userDetails.getUserId();

        userLoginService.processLogin(userId);

        String accessToken = jwtProvider.createAccessToken(userId, userDetails.getRole());
        String refreshToken = jwtProvider.createRefreshToken(userId);

        redisTokenService.saveRefreshTokenHash(
                userId,
                refreshToken,
                jwtProvider.getRemainingValidityMillis(refreshToken)
        );

        int refreshTokenMaxAge =
                (int) (jwtProvider.getRemainingValidityMillis(refreshToken) / 1000);

        cookieManager.addRefreshTokenCookie(response, refreshToken, refreshTokenMaxAge);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path(successRedirectPath)
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}