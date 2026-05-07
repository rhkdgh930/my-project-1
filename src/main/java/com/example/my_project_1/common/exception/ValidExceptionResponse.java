package com.example.my_project_1.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Schema(description = "Bean validation 실패 응답")
public class ValidExceptionResponse {
    private static final String VALIDATION_CODE = "VALIDATION_ERROR";

    @Schema(description = "HTTP status code", example = "400")
    private final int status;

    @Schema(description = "Validation error code", example = "VALIDATION_ERROR")
    private final String code;

    @Schema(description = "Validation failure message", example = "입력 값이 유효하지 않습니다.")
    private final String message;

    @Schema(description = "Field name to validation messages map")
    private final Map<String, List<String>> errors;

    public ValidExceptionResponse(ErrorCode errorCode, Map<String, List<String>> errors) {
        this.status = errorCode.getHttpStatus().value();
        this.code = VALIDATION_CODE;
        this.message = errorCode.getMessage();
        this.errors = errors;
    }
}
