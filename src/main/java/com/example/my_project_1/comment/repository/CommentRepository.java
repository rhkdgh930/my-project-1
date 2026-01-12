package com.example.my_project_1.comment.repository;

import com.example.my_project_1.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostIdAndDeletedFalseOrderByIdAsc(Long postId);

    List<Comment> findAllByParentIdAndDeletedFalseOrderByIdAsc(Long parentId);

    Optional<Comment> findByIdAndDeletedFalse(Long id);

    boolean existsByParentId(Long parentId);
}
