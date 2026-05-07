package com.example.my_project_1.auth.handler;

import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ErrorResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn(
                "[SECURITY][JwtAccessDeniedHandler][ACCESS_DENIED] uri={} ip={} method={}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getMethod()
        );

        errorResponseWriter.write(response, ErrorCode.ACCESS_DENIED);
    }

}
