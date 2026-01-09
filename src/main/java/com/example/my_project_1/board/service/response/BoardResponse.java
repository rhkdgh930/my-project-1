package com.example.my_project_1.board.service.response;

import com.example.my_project_1.board.domain.Board;
import lombok.Getter;

@Getter
public class BoardResponse {
    private Long boardId;
    private String name;
    private String description;

    public static BoardResponse from(Board board) {
        BoardResponse response = new BoardResponse();
        response.boardId = board.getId();
        response.name = board.getName();
        response.description = board.getDescription();
        return response;
    }
}
