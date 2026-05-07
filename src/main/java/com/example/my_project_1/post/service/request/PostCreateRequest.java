package com.example.my_project_1.post.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "게시글 작성 요청")
public class PostCreateRequest {
    @Schema(description = "게시글 제목", example = "첫 번째 게시글", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String title;

    @Schema(description = "게시글 본문. Markdown 이미지 URL은 작성 후 이미지 attach 대상이 될 수 있습니다.",
            example = "본문 내용입니다.\\n\\n![image](/images/550e8400-e29b-41d4-a716-446655440000.png)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String content;
}
