package com.example.my_project_1.board.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "게시판 수정 요청")
public class BoardUpdateRequest {
    @Schema(title = "수정할 게시판 이름", description = "중복되지 않는 고유한 게시판 이름입니다.",
            example = "Q&A 게시판", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(title = "수정할 게시판 설명", description = "게시판의 용도 및 이용 규칙 설명입니다.",
            example = "궁금한 점을 질문하는 공간입니다.")
    private String description;
}
