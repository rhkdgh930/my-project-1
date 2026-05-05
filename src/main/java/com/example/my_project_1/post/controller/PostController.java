package com.example.my_project_1.post.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boards/{boardId}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;

    @PostMapping
    public PostDetailResponse create(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.create(boardId, userId, request);
    }

    @PatchMapping("/{postId}")
    public PostDetailResponse update(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.update(boardId, postId, userId, request);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        postCommandService.delete(boardId, postId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public PageResponse<PostListResponse> getPosts(
            @PathVariable Long boardId,
            Pageable pageable
    ) {
        return postQueryService.getPosts(boardId, pageable);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse read(
            @PathVariable Long boardId,
            @PathVariable Long postId) {
        return postQueryService.getPostDetail(boardId, postId);
    }

    @PostMapping("/{postId}/like")
    public boolean like(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        return postCommandService.like(boardId, postId, userId);
    }
}
