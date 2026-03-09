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

    @Operation(summary = "게시판 생성", description = "새로운 게시판을 생성합니다. (관리자 전용)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시판 생성 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 게시판 이름 혹은 유효하지 않은 입력값"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        BoardResponse response = boardCommandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "게시판 수정", description = "기존 게시판의 정보를 수정합니다. (관리자 전용)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시판 수정 성공"),
            @ApiResponse(responseCode = "400", description = "중복된 게시판 이름 혹은 유효하지 않은 입력값"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)")
    })
    @PatchMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BoardResponse> update(
            @PathVariable Long boardId,
            @RequestBody @Valid BoardUpdateRequest request) {
        BoardResponse update = boardCommandService.update(boardId, request);
        return ResponseEntity.ok(update);
    }

    @Operation(summary = "게시판 삭제 (Soft Delete)", description = "게시판을 삭제 처리합니다. 데이터는 마스킹되어 보존됩니다. (관리자 전용)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시판 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN 권한 필요)")
    })
    @DeleteMapping("/{boardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long boardId) {
        boardCommandService.delete(boardId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "단일 게시판 상세 조회", description = "특정 ID의 게시판 정보를 조회합니다. (삭제된 게시판 제외)")
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long boardId) {
        BoardResponse response = boardQueryService.findBoardById(boardId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 게시판 조회 (관리자용)", description = "삭제된 게시판을 포함하여 모든 게시판 목록을 조회합니다. (관리자 전용)")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BoardResponse>> getAllBoardsForAdmin() {
        List<BoardResponse> responses = boardQueryService.findAllBoardsForAdmin();
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "활성 게시판 목록 조회", description = "삭제되지 않은 활성 상태의 모든 게시판 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        List<BoardResponse> responses = boardQueryService.findAllBoards();
        return ResponseEntity.ok(responses);
    }
}
