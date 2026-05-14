package com.example.my_project_1.common.exception;

import com.example.my_project_1.common.utils.DataSerializer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ErrorResponseWriter {

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        write(response, errorCode, null);
    }

    public void write(
            HttpServletResponse response,
            ErrorCode errorCode,
            Object data
    ) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(
                DataSerializer.serialize(new ExceptionResponse(errorCode, data))
        );
    }
}