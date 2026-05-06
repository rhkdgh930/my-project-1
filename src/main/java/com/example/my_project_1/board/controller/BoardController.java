package com.example.my_project_1.board.controller;

import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;
import com.example.my_project_1.common.exception.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Tag(name = "Board API", description = "공개 게시판 조회 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    @Operation(
            summary = "게시판 상세 조회",
            description = "삭제되지 않은 게시판 단건을 조회합니다. Public API입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시판 조회 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시판 없음 또는 삭제된 게시판",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardResponse> getBoard(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId) {
        BoardResponse response = boardQueryService.findBoardById(boardId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시판 목록 조회",
            description = "삭제되지 않은 활성 게시판 목록을 조회합니다. Public API입니다."
    )
    @ApiResponse(responseCode = "200", description = "게시판 목록 조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoardResponse.class))))
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoards() {
        List<BoardResponse> responses = boardQueryService.findAllBoards();
        return ResponseEntity.ok(responses);
    }
}
