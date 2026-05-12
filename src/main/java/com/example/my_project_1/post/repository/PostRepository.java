package com.example.my_project_1.post.repository;

import com.example.my_project_1.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    @Modifying
    @Query("""
            update Post p
            set p.viewCount = :view,
                p.likeCount = :like
            where p.id = :postId
            """)
    void updateCounts(Long postId, long view, long like);

    @Modifying
    @Query("""
            update Post p
            set p.viewCount = :viewCount
            where p.id = :postId
            """)
    void updateViewCount(Long postId, long viewCount);

    @Modifying
    @Query("""
            update Post p
            set p.likeCount =
                case
                    when p.likeCount + :delta < 0 then 0
                    else p.likeCount + :delta
                end
            where p.id = :postId
            """)
    int updateLikeCountDelta(@Param("postId") Long postId, @Param("delta") long delta);

    @Query("""
            select p.likeCount
            from Post p
            where p.id = :postId
            """)
    Optional<Long> findLikeCountById(@Param("postId") Long postId);

    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deletedAt is null
              and p.board.deletedAt is null
            """)
    Optional<Post> findActiveById(@Param("postId") Long postId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Post p
            where p.id = :postId
              and p.deletedAt is null
              and p.board.deletedAt is null
            """)
    Optional<Post> findActiveByIdForUpdate(@Param("postId") Long postId);

    @Query("""
            select p
            from Post p
            where p.board.id = :boardId
              and p.deletedAt is null
              and p.board.deletedAt is null
            """)
    Page<Post> findAllActiveByBoardId(@Param("boardId") Long boardId, Pageable pageable);
}
