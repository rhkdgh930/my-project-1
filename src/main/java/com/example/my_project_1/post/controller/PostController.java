package com.example.my_project_1.post.controller;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.PostRedisService;
import com.example.my_project_1.post.service.PostCommandService;
import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostListResponse;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards/{boardId}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final PostRedisService postRedisService;

    @PostMapping
    public PostDetailResponse create(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PostCreateRequest request
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return postCommandService.create(boardId, userId, request);
    }

    @PatchMapping("/{postId}")
    public PostDetailResponse update(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid PostUpdateRequest request
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return postCommandService.update(boardId, postId, userId, request);
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
        postRedisService.increaseView(postId);
        return postQueryService.getPostDetail(boardId, postId);
    }

    @PostMapping("/{postId}/like")
    public boolean like(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return postCommandService.like(boardId, postId, userId);
    }
}