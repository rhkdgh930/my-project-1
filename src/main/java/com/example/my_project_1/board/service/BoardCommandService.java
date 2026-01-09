package com.example.my_project_1.board.service;

import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;

public interface BoardCommandService {
    BoardResponse create(BoardCreateRequest request);

    BoardResponse update(Long boardId, BoardUpdateRequest request);

    void delete(Long boardId);

}
