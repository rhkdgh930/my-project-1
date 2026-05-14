package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from PostLike pl
            where pl.postId = :postId
              and pl.userId = :userId
            """)
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
