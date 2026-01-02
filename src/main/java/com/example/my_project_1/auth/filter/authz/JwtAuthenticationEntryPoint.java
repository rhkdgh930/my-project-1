package com.example.my_project_1.auth.filter.authz;

import com.example.my_project_1.auth.filter.JwtAuthenticationException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.INVALID_ACCESS_TOKEN;

        if (authenticationException instanceof JwtAuthenticationException jwtEx) {
            errorCode = jwtEx.getErrorCode();
        }

        log.warn(
                "[JwtAuthenticationEntryPoint.commence] 401 Unauthorized | uri={} | message={}",
                request.getRequestURI(),
                errorCode.getMessage()
        );

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                DataSerializer.serialize(new ExceptionResponse(errorCode))
        );
    }
}
