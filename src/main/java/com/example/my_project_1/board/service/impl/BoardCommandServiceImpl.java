package com.example.my_project_1.board.service.impl;

import com.example.my_project_1.board.domain.Board;
import com.example.my_project_1.board.repository.BoardRepository;
import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@RequiredArgsConstructor
@Service
public class BoardCommandServiceImpl implements BoardCommandService {
    private final BoardRepository boardRepository;

    @Override
    public BoardResponse create(BoardCreateRequest request) {
        validateDuplicateName(request.getName());

        Board board = Board.create(
                request.getName(),
                request.getDescription()
        );
        return BoardResponse.from(boardRepository.save(board));
    }

    @Override
    public BoardResponse update(Long boardId, BoardUpdateRequest request) {
        Board board = findBoard(boardId);

        if (!board.getName().equals(request.getName())) {
            validateDuplicateName(request.getName());
        }

        board.update(
                request.getName(),
                request.getDescription()
        );
        return BoardResponse.from(board);
    }

    private void validateDuplicateName(String name) {
        if (boardRepository.existsByName(name)) {
            throw new CustomException(ErrorCode.ALREADY_EXIST_BOARD_NAME);
        }
    }

    @Override
    public void delete(Long boardId) {
        Board board = findBoard(boardId);
        board.delete();
    }

    private Board findBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }
}
