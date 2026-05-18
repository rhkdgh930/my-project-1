package com.example.my_project_1.post.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "게시글 수정 요청")
public class PostUpdateRequest {
    @Schema(description = "수정할 게시글 제목", example = "수정된 게시글 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String title;

    @Schema(description = "수정할 게시글 본문. Markdown 이미지 URL은 수정 후 이미지 sync 대상이 될 수 있습니다.",
            example = "수정된 본문 내용입니다.\\n\\n![image](/images/550e8400-e29b-41d4-a716-446655440000.png)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String content;

    @Schema(description = "교체할 게시글 태그 목록. null이면 빈 목록처럼 처리합니다.", example = "[\"Spring\", \"JPA\"]")
    private List<String> tags;
}
