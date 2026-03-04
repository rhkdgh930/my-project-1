package com.example.my_project_1.post.service;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.response.PostDetailResponse;
import com.example.my_project_1.post.service.response.PostListResponse;
import org.springframework.data.domain.Pageable;

public interface PostQueryService {
    PageResponse<PostListResponse> getPosts(Long boardId, Pageable pageable);

    PostDetailResponse getPostDetail(Long boardId, Long postId);
}
