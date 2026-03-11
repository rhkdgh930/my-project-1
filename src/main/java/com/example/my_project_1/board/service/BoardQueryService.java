package com.example.my_project_1.board.service;

import com.example.my_project_1.board.service.response.BoardResponse;

import java.util.List;

public interface BoardQueryService {
    List<BoardResponse> findAllBoardsForAdmin();

    List<BoardResponse> findAllBoards();

    BoardResponse findBoardById(Long boardId);

}
