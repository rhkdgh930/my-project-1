package com.example.my_project_1.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ExceptionResponse {
    private final int status;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL) // 데이터가 있을 때만 포함
    private final Object data;

    public ExceptionResponse(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ExceptionResponse(ErrorCode errorCode, Object data) {
        this.status = errorCode.getHttpStatus().value();
        this.message = errorCode.getMessage();
        this.code = errorCode.name();
        this.data = data;
    }
}
