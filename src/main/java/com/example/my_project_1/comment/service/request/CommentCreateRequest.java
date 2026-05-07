package com.example.my_project_1.comment.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "댓글/대댓글 작성 요청")
public class CommentCreateRequest {
    @Schema(description = "댓글 내용. null/blank일 수 없고 최대 1000자입니다.",
            example = "댓글 내용입니다.",
            maxLength = 1000,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 1000)
    private String content;
}
