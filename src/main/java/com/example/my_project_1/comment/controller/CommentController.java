package com.example.my_project_1.comment.controller;

import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
import com.example.my_project_1.comment.service.request.CommentCreateRequest;
import com.example.my_project_1.comment.service.request.CommentUpdateRequest;
import com.example.my_project_1.comment.service.response.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;

    @PostMapping
    public void write(
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.writeComment(postId, userId, request.getContent());
    }

    @PostMapping("/{commentId}/replies")
    public void reply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.writeReply(postId, commentId, userId, request.getContent());
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        return commentCommandService.update(postId, commentId, userId, request.getContent());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long userId = userDetails.getUserId();
        commentCommandService.delete(postId, commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<CommentResponse> get(@PathVariable Long postId) {
        return commentQueryService.getComments(postId);
    }
}
