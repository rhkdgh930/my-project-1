package com.example.my_project_1.common.exception;

import lombok.Getter;

@Getter
public class ExceptionResponse {
    private final int status;
    private final String code;
    private final String message;

    public ExceptionResponse(ErrorCode errorCode) {
        this.status = errorCode.getHttpStatus().value();
        this.code = errorCode.name();
        this.message = errorCode.getMessage();
    }
}
