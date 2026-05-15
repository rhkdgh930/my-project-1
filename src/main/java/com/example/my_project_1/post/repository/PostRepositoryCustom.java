package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import com.example.my_project_1.post.service.request.PostSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {

    Page<Post> searchActivePosts(
            Long boardId,
            PostSearchCondition condition,
            Pageable pageable
    );

    Page<Post> findLikedActivePostsByUserId(Long userId, Pageable pageable);

    Page<Post> findActivePostsByUserId(Long userId, Pageable pageable);

    Page<Post> findCommentedActivePostsByUserId(Long userId, Pageable pageable);

    List<Post> findPopularActivePosts(Long boardId, int size);
}
