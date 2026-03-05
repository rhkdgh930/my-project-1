package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.auth.utils.CookieUtils;
import com.example.my_project_1.common.utils.DataSerializer;
import com.example.my_project_1.user.service.UserLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final RedisUserContextService redisUserContextService;
    private final RedisLoginAttemptService loginAttemptService;
    private final UserLoginService userLoginService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = principal.getUserId();
        String email = principal.getEmail();

        loginSuccess(email);
        userLoginService.updateLastLogin(userId);

        CachedUserContext ctx = redisUserContextService.getUserContext(userId);
        redisUserContextService.validateActiveUser(ctx);

        String accessToken =
                jwtProvider.createAccessToken(userId, principal.getRole());

        String refreshToken =
                jwtProvider.createRefreshToken(userId);

        redisTokenService.saveRefreshTokenHash(
                userId,
                refreshToken,
                jwtProvider.getRemainingValidityMillis(refreshToken)
        );

        int refreshMaxAge = (int) (jwtProvider.getRemainingValidityMillis(refreshToken) / 1000);
        CookieUtils.addCookie(response, "refreshToken", refreshToken, refreshMaxAge);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                DataSerializer.serialize(new TokenResponse(accessToken, refreshToken))
        );
    }

    private void loginSuccess(String email) {
        if (email != null) {
            loginAttemptService.loginSucceeded(email);
        }
    }
}
