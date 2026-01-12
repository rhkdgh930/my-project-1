package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.service.response.CommentResponse;

import java.util.List;

public interface CommentQueryService {
    List<CommentResponse> getComments(Long postId);
}
