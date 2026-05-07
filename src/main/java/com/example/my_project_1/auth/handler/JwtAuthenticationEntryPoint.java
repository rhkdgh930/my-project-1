package com.example.my_project_1.auth.handler;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ErrorResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.INVALID_ACCESS_TOKEN;

        if (authenticationException instanceof JwtAuthenticationException jwtEx) {
            errorCode = jwtEx.getErrorCode();
        }

        log.warn(
                "[SECURITY][JwtAuthenticationEntryPoint][UNAUTHORIZED] uri={} ip={} method={} error={}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getMethod(),
                errorCode.name()
        );

        errorResponseWriter.write(response, errorCode);
    }
}
