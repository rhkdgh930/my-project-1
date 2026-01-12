package com.example.my_project_1.comment.controller;

import com.example.my_project_1.comment.service.CommentCommandService;
import com.example.my_project_1.comment.service.CommentQueryService;
import com.example.my_project_1.comment.service.request.CommentCreateRequest;
import com.example.my_project_1.comment.service.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        commentCommandService.writeComment(postId, userId, request.getContent());
    }

    @PostMapping("/{commentId}/replies")
    public void reply(
            @PathVariable Long commentId,
            @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.valueOf(userDetails.getUsername());
        commentCommandService.writeReply(commentId, userId, request.getContent());
    }

    @GetMapping
    public List<CommentResponse> get(@PathVariable Long postId) {
        return commentQueryService.getComments(postId);
    }
}
