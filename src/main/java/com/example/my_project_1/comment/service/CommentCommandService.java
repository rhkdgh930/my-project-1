package com.example.my_project_1.comment.service;

public interface CommentCommandService {
    Long writeComment(Long postId, Long userId, String content);

    Long writeReply(Long parentId, Long userId, String content);

    void update(Long commentId, Long userId, String content);

    void delete(Long commentId, Long userId);

}
