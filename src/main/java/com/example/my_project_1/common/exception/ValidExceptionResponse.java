package com.example.my_project_1.common.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ValidExceptionResponse {
    private static final String VALIDATION_CODE = "VALIDATION_ERROR";

    private final int status;
    private final String code;
    private final String message;
    private final Map<String, List<String>> errors;

    public ValidExceptionResponse(ErrorCode errorCode, Map<String, List<String>> errors) {
        this.status = errorCode.getHttpStatus().value();
        this.code = VALIDATION_CODE;
        this.message = errorCode.getMessage();
        this.errors = errors;
    }
}
