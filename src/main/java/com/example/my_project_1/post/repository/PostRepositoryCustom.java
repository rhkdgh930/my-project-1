package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {

    Page<Post> searchActivePosts(
            Long boardId,
            PostSearchCondition condition,
            Pageable pageable
    );

    Page<Post> findLikedActivePostsByUserId(Long userId, Pageable pageable);
}
