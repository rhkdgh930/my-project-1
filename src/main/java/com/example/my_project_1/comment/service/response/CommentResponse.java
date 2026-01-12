package com.example.my_project_1.comment.service.response;

import com.example.my_project_1.comment.domain.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class CommentResponse {
    private final Long id;
    private final Long authorId;
    private final String content;
    private final List<CommentResponse> replies = new ArrayList<>();

    private CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.authorId = comment.getAuthorId();
        this.content = comment.getContent();
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment);
    }

    public void addReply(CommentResponse reply) {
        replies.add(reply);
    }
}
