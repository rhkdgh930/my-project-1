package com.example.my_project_1.auth.filter.authn;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.response.TokenResponse;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.auth.utils.JwtProvider;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        String accessToken = jwtProvider.createAccessToken(
                principal.getUserId(),
                principal.getUsername(),
                principal.getRole()
        );

        String refreshToken = jwtProvider.createRefreshToken(principal.getUsername());

        redisTokenService.saveRefreshToken(
                principal.getUsername(),
                refreshToken,
                jwtProvider.getRemainingValidityMillis(refreshToken)
        );

        response.setContentType("application/json");
        response.getWriter().write(
                DataSerializer.serialize(new TokenResponse(accessToken, refreshToken))
        );
    }
}
