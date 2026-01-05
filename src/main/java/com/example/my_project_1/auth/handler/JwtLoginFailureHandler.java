package com.example.my_project_1.auth.handler;

import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtLoginFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        ErrorCode errorCode;

        if (exception instanceof LockedException) {
            errorCode = ErrorCode.USER_SUSPENDED;
        } else if (exception instanceof DisabledException) {
            errorCode = ErrorCode.USER_NOT_FOUND;
        } else if (exception instanceof BadCredentialsException) {
            errorCode = ErrorCode.INVALID_CREDENTIALS;
        } else {
            errorCode = ErrorCode.AUTHENTICATION_FAILED;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                DataSerializer.serialize(new ExceptionResponse(errorCode))
        );
    }
}
