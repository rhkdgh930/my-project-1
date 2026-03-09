package com.example.my_project_1.board.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BoardCreateRequest {
    @Schema(title = "게시판 이름", description = "중복되지 않는 고유한 게시판 이름",
            example = "자유게시판", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(title = "게시판 설명", description = "게시판의 용도 및 이용 규칙 설명",
            example = "누구나 자유롭게 소통하는 공간입니다.")
    private String description;

    public static BoardCreateRequest create(String name, String description) {
        BoardCreateRequest request = new BoardCreateRequest();
        request.name = name;
        request.description = description;
        return request;
    }
}
