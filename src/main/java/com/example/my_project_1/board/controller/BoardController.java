package com.example.my_project_1.board.controller;

import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board API", description = "게시판 생성, 수정, 삭제 및 조회 기능")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    @Operation(summary = "단일 게시판 상세 조회", description = "특정 ID의 게시판 정보를 조회합니다. (삭제된 게시판 제외)")
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long boardId) {
        BoardResponse response = boardQueryService.findBoardById(boardId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "활성 게시판 목록 조회", description = "삭제되지 않은 활성 상태의 모든 게시판 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        List<BoardResponse> responses = boardQueryService.findAllBoards();
        return ResponseEntity.ok(responses);
    }
}
