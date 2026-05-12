package com.example.my_project_1.post.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.ExceptionResponse;
import com.example.my_project_1.common.exception.ValidExceptionResponse;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostLikeResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post API", description = "게시글 조회, 작성, 수정, 삭제, 좋아요 API")
@RestController
@RequestMapping("/api/boards/{boardId}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;

    @Operation(
            summary = "게시글 작성",
            description = "활성 Board에 게시글을 작성합니다. 작성 후 본문 이미지 attach는 POST_CREATED Outbox 후속 작업으로 처리됩니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 작성 성공",
                    content = @Content(schema = @Schema(implementation = PostDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패",
                    content = @Content(schema = @Schema(implementation = ValidExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시판 없음 또는 삭제된 게시판",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public PostDetailResponse create(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.create(boardId, userId, request);
    }

    @Operation(
            summary = "게시글 수정",
            description = "작성자만 활성 게시글을 수정할 수 있습니다. 삭제된 Board 아래 Post는 수정 대상이 아닙니다. 본문 이미지 sync는 POST_UPDATED Outbox 후속 작업으로 처리됩니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 수정 성공",
                    content = @Content(schema = @Schema(implementation = PostDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "validation 실패 또는 board-post 관계 불일치",
                    content = @Content(schema = @Schema(oneOf = {ValidExceptionResponse.class, ExceptionResponse.class}))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 아님",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{postId}")
    public PostDetailResponse update(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.update(boardId, postId, userId, request);
    }

    @Operation(
            summary = "게시글 삭제",
            description = "작성자만 활성 게시글을 hidden soft delete 처리할 수 있습니다. 성공 시 title/content를 마스킹하고 deletedAt을 세팅하며, 이미지 detach는 POST_DELETED Outbox 후속 작업으로 처리됩니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "board-post 관계 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자 아님",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        postCommandService.delete(boardId, postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "게시글 목록 조회",
            description = """
                삭제되지 않은 Board의 활성 게시글 목록을 페이징 조회합니다.
                keyword/searchType으로 제목 또는 본문 검색이 가능하고,
                sortType으로 최신순/오래된순/조회수순/좋아요순 정렬이 가능합니다.
                조회수/좋아요 정렬은 DB에 마지막 동기화된 count 기준이며,
                응답의 viewCount/likeCount는 Redis 최신값으로 보정될 수 있습니다.
                Public API입니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시판 없음 또는 삭제된 게시판",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public PageResponse<PostListResponse> getPosts(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @ParameterObject PostSearchCondition condition,
            @ParameterObject Pageable pageable
    ) {
        return postQueryService.getPosts(boardId, condition, pageable);
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "활성 게시글 상세를 조회합니다. Public API입니다. 조회 시 view count가 증가하며 viewCount/likeCount는 응답 시점 기준 값입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시글 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = PostDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "board-post 관계 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{postId}")
    public PostDetailResponse read(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId) {
        return postQueryService.getPostDetail(boardId, postId);
    }

    @Operation(
            summary = "게시글 좋아요 토글",
            description = "활성 게시글의 좋아요를 토글합니다. 삭제된 Board 아래 Post는 좋아요 대상이 아닙니다. 응답은 토글 후 좋아요 여부와 응답 시점 기준 likeCount입니다.",
            security = @SecurityRequirement(name = "jwtAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좋아요 토글 성공",
                    content = @Content(schema = @Schema(implementation = PostLikeResponse.class))),
            @ApiResponse(responseCode = "400", description = "board-post 관계 불일치",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시글 없음, 삭제된 게시글, 또는 삭제된 Board 아래 게시글",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{postId}/like")
    public PostLikeResponse like(
            @Parameter(description = "게시판 ID", example = "1", required = true)
            @PathVariable Long boardId,
            @Parameter(description = "게시글 ID", example = "10", required = true)
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.like(boardId, postId, userId);
    }
}
