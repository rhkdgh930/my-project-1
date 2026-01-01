package com.example.my_project_1.auth.filter.authz;

import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        log.warn("URL 인가 실패 {}", accessDeniedException.getMessage());
        response.getWriter().write(
                DataSerializer.serialize(new ExceptionResponse(errorCode))
        );
    }
}
