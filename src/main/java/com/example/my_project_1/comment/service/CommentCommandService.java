package com.example.my_project_1.comment.service;

public interface CommentCommandService {
    Long writeComment(Long postId, Long authorId, String content);

    Long writeReply(Long parentId, Long authorId, String content);

    void update(Long commentId, Long userId, String content);

    void delete(Long commentId, Long userId);

}
