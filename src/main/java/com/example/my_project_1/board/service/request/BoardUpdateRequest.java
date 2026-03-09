package com.example.my_project_1.board.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BoardUpdateRequest {
    @Schema(title = "수정할 게시판 이름", example = "Q&A 게시판", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(title = "수정할 게시판 설명", example = "궁금한 점을 질문하는 공간입니다.")
    private String description;
}
