package com.example.my_project_1.auth.filter;

import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);

        } catch (AccessDeniedException | AuthenticationException ex) {
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "[ExceptionHandlerFilter.doFilterInternal] 500 Internal Server Error | uri={}",
                    request.getRequestURI(),
                    ex
            );
            sendExceptionResponse(response);
        }
    }

    private void sendExceptionResponse(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(DataSerializer.serialize(new ExceptionResponse(ErrorCode.INTERNAL_SERVER_ERROR)));
    }
}
