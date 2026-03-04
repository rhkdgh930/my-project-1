package com.example.my_project_1.board.controller;

import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        BoardResponse response = boardCommandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Long boardId,
            @RequestBody @Valid BoardUpdateRequest request) {
        BoardResponse update = boardCommandService.update(boardId, request);
        return ResponseEntity.ok(update);
    }

    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long boardId) {
        boardCommandService.delete(boardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long boardId) {
        BoardResponse response = boardQueryService.findBoardById(boardId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllActiveBoards() {
        List<BoardResponse> responses = boardQueryService.findAllActiveBoards();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        List<BoardResponse> responses = boardQueryService.findAllBoards();
        return ResponseEntity.ok(responses);
    }
}
