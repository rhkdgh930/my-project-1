package com.example.my_project_1.post.service;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostQueryService {
    PageResponse<PostListResponse> getPosts(
            Long boardId,
            PostSearchCondition condition,
            Pageable pageable
    );
    PageResponse<PostListResponse> getLikedPosts(Long userId, Pageable pageable);
    PageResponse<PostListResponse> getMyPosts(Long userId, Pageable pageable);
    PageResponse<PostListResponse> getCommentedPosts(Long userId, Pageable pageable);
    List<PostListResponse> getPopularPosts(Long boardId, int size);
    PostDetailResponse getPostDetail(Long boardId, Long postId);
    PostDetailResponse getPostDetail(Long boardId, Long postId, Long currentUserId);
}
