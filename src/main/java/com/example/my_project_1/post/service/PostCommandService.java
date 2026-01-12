package com.example.my_project_1.post.service;

import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;

public interface PostCommandService {
    PostDetailResponse create(Long boardId, Long userId, PostCreateRequest request);
    PostDetailResponse update(Long boardId, Long postId, Long userId, PostUpdateRequest request);
    boolean like(Long boardId, Long postId, Long userId);
}
