package com.example.my_project_1.auth.filter;

import com.example.my_project_1.auth.exception.LoginFailException;
import com.example.my_project_1.auth.service.RedisLoginAttemptService;
import com.example.my_project_1.auth.service.request.LoginRequest;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final RedisLoginAttemptService loginAttemptService;

    public JwtLoginFilter(
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler,
            RedisLoginAttemptService loginAttemptService
    ) {
        setAuthenticationManager(authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        this.loginAttemptService = loginAttemptService;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            LoginRequest loginRequest =
                    DataSerializer.deserialize(request.getInputStream(), LoginRequest.class);

            if (loginRequest == null
                    || !StringUtils.hasText(loginRequest.getEmail())
                    || !StringUtils.hasText(loginRequest.getPassword())) {
                throw new AuthenticationServiceException("이메일과 비밀번호는 필수입니다.");
            }

            String email = normalizeEmail(loginRequest.getEmail());
            request.setAttribute("email", email);

            if (loginAttemptService.isBlocked(email)) {
                throw new LoginFailException(
                        "너무 많은 로그인 시도 실패로 인해 계정이 일시 차단되었습니다, 10분뒤에 다시 시도해주세요."
                );
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            loginRequest.getPassword()
                    );

            return getAuthenticationManager().authenticate(authToken);
        } catch (IOException | IllegalStateException e) {
            throw new AuthenticationServiceException("로그인 요청 파싱 실패", e);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}