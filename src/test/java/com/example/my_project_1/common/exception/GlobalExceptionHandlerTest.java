package com.example.my_project_1.common.exception;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("JwtAuthenticationException은 ErrorCode 상태와 ExceptionResponse로 응답한다.")
    void handleJwtAuthenticationException_returnsErrorCodeResponse() {
        ErrorCode errorCode = ErrorCode.USER_SUSPENDED;

        ResponseEntity<ExceptionResponse> response =
                handler.handleJwtAuthenticationException(new JwtAuthenticationException(errorCode));

        assertThat(response.getStatusCode().value()).isEqualTo(errorCode.getHttpStatus().value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(errorCode.getHttpStatus().value());
        assertThat(response.getBody().getCode()).isEqualTo(errorCode.name());
        assertThat(response.getBody().getMessage()).isEqualTo(errorCode.getMessage());
    }
}
