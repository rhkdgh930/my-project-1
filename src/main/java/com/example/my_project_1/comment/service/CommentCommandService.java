package com.example.my_project_1.comment.service;

import com.example.my_project_1.comment.service.response.CommentResponse;

public interface CommentCommandService {
    Long writeComment(Long postId, Long userId, String content);

    Long writeReply(Long postId, Long parentId, Long userId, String content);

    CommentResponse update(Long postId, Long commentId, Long userId, String content);

    void delete(Long postId, Long commentId, Long userId);

}
