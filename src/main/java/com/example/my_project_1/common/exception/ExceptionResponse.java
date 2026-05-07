package com.example.my_project_1.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "공통 예외 응답")
public class ExceptionResponse {
    @Schema(description = "HTTP status code", example = "404")
    private final int status;

    @Schema(description = "ErrorCode enum name", example = "POST_NOT_FOUND")
    private final String code;

    @Schema(description = "Client-facing error message", example = "존재하지 않는 게시물입니다.")
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Additional error data. Present only when the error has extra context.", nullable = true)
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
