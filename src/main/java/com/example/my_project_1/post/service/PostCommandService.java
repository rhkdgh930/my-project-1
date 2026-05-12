package com.example.my_project_1.post.service;

import com.example.my_project_1.post.service.request.PostCreateRequest;
import com.example.my_project_1.post.service.request.PostUpdateRequest;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostLikeResponse;

public interface PostCommandService {
    PostDetailResponse create(Long boardId, Long userId, PostCreateRequest request);
    PostDetailResponse update(Long boardId, Long postId, Long userId, PostUpdateRequest request);
    void delete(Long boardId, Long postId, Long userId);
    PostLikeResponse like(Long boardId, Long postId, Long userId);
    PostLikeResponse likeIdempotently(Long boardId, Long postId, Long userId);
    PostLikeResponse unlikeIdempotently(Long boardId, Long postId, Long userId);
}
