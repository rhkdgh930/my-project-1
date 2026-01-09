package com.example.my_project_1.board.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.domain.BoardStatus;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.board.service.response.BoardResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class BoardQueryServiceImpl implements BoardQueryService {
    private final BoardRepository boardRepository;

    @Override
    public List<BoardResponse> findAllBoards() {
        List<Board> boards = boardRepository.findAllByBoardStatus(BoardStatus.ACTIVE);
        return boards.stream()
                .map(BoardResponse::from)
                .toList();
    }

    @Override
    public BoardResponse findBoardById(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        return BoardResponse.from(board);
    }
}
