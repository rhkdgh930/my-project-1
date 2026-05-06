package com.example.my_project_1.comment.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
import com.example.my_project_1.comment.service.request.CommentCreateRequest;
import com.example.my_project_1.comment.service.request.CommentUpdateRequest;
import com.example.my_project_1.comment.service.response.CommentResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment API", description = "댓글/대댓글 조회, 작성, 수정, 삭제 API")
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;

    @Operation(
            summary = "댓글 작성",
            description = "활성 Post에 루트 댓글을 작성합니다. active post는 post.deletedAt IS NULL AND post.board.deletedAt IS NULL 조건입니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "validation 실패",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public void write(
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.writeComment(postId, userId, request.getContent());
    }

    @Operation(
            summary = "대댓글 작성",
            description = "활성 Post에서 댓글에 대댓글을 작성합니다. 대댓글은 depth 1까지만 허용되며, URL postId와 parent comment postId가 일치해야 합니다. 삭제된 댓글에는 대댓글을 작성할 수 없습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "validation 실패, post-comment 관계 불일치, 삭제된 댓글, 또는 depth 초과",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 또는 부모 댓글 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{commentId}/replies")
    public void reply(
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @Parameter(description = "부모 댓글 ID", example = "1", required = true)
            @PathVariable Long commentId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.writeReply(postId, commentId, userId, request.getContent());
    }

    @Operation(
            summary = "댓글 수정",
            description = "작성자만 활성 Post의 댓글을 수정할 수 있습니다. URL postId와 comment.postId가 일치해야 하며, 삭제된 댓글은 수정할 수 없습니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패, post-comment 관계 불일치, 또는 삭제된 댓글",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 아님",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 또는 댓글 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1", required = true)
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        return commentCommandService.update(postId, commentId, userId, request.getContent());
    }

    @Operation(
            summary = "댓글 삭제",
            description = "작성자만 활성 Post의 댓글을 tombstone 처리할 수 있습니다. 삭제 성공 응답은 204 No Content입니다. 같은 작성자가 이미 삭제한 댓글을 다시 삭제하면 idempotent하게 204로 응답합니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "post-comment 관계 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 아님. 남의 삭제된 댓글 삭제 시도도 ACCESS_DENIED입니다.",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 또는 댓글 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1", required = true)
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.delete(postId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "활성 Post의 댓글/대댓글 트리를 조회합니다. Public API입니다. 삭제된 댓글은 tombstone 형태로 포함되며 authorId=null, author=null로 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class)))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public List<CommentResponse> get(
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId) {
        return commentQueryService.getComments(postId);
    }
}
