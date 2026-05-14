package com.example.my_project_1.board.controller;

import com.example.my_project_1.board.service.BoardCommandService;
import com.example.my_project_1.board.service.BoardQueryService;
import com.example.my_project_1.board.service.request.BoardCreateRequest;
import com.example.my_project_1.board.service.request.BoardUpdateRequest;
import com.example.my_project_1.board.service.response.BoardResponse;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Board API", description = "관리자 전용 Board API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/boards")
public class AdminBoardController {

    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    @Operation(
            summary = "게시판 생성",
            description = "새로운 게시판을 생성합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시판 생성 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패 또는 중복된 게시판 이름",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        BoardResponse response = boardCommandService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "게시판 수정",
            description = "삭제되지 않은 게시판의 이름과 설명을 수정합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시판 수정 성공",
                    content = @Content(schema = @Schema(implementation = BoardResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패 또는 중복된 게시판 이름",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시판 없음 또는 삭제된 게시판",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardResponse> update(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @RequestBody @Valid BoardUpdateRequest request) {
        BoardResponse update = boardCommandService.update(boardId, request);
        return ResponseEntity.ok(update);
    }

    @Operation(
            summary = "게시판 삭제",
            description = "게시판을 hidden soft delete 처리합니다. 삭제 시 name은 deleted_{uuid}_... 형태로 변경되어 unique 충돌을 줄이고, 삭제된 Board는 일반 Board 조회와 active post query에서 제외됩니다. 성공 응답은 현재 코드 기준 200입니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시판 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시판 없음 또는 이미 삭제된 게시판",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId) {
        boardCommandService.delete(boardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "전체 게시판 조회",
            description = "삭제된 게시판을 포함하여 모든 게시판 목록을 조회합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 게시판 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoardResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 필요",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getAllBoardsForAdmin() {
        List<BoardResponse> responses = boardQueryService.findAllBoardsForAdmin();
        return ResponseEntity.ok(responses);
    }
}
