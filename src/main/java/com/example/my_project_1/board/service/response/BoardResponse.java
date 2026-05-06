package com.example.my_project_1.board.service.response;

import com.example.my_project_1.board.domain.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "게시판 응답")
public class BoardResponse {
    @Schema(description = "게시판 ID", example = "1")
    private Long boardId;

    @Schema(description = "게시판 이름", example = "자유게시판")
    private String name;

    @Schema(description = "게시판 설명", example = "누구나 자유롭게 소통하는 공간입니다.")
    private String description;

    @Schema(description = "게시판 생성 시각", example = "2026-05-06T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "게시판 수정 시각", example = "2026-05-06T10:30:00")
    private LocalDateTime updatedAt;

    public static BoardResponse from(Board board) {
        BoardResponse response = new BoardResponse();
        response.boardId = board.getId();
        response.name = board.getName();
        response.description = board.getDescription();
        response.createdAt = board.getCreatedAt();
        response.updatedAt = board.getUpdatedAt();
        return response;
    }
}
