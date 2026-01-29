package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Modifying
    @Query("""
            update Post p
            set p.viewCount = :view,
                p.likeCount = :like
            where p.id = :postId
            """)
    void updateCounts(Long postId, long view, long like);

    Optional<Post> findByIdAndDeletedAtIsNull(Long postId);

    Page<Post> findAllByBoardIdAndDeletedAtIsNull(Long boardId, Pageable pageable);
}
