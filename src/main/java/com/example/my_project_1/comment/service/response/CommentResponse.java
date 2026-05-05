package com.example.my_project_1.comment.service.response;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.user.client.AuthorSummary;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommentResponse {
    private final Long id;
    private final Long authorId;
    private final AuthorSummary author;
    private final String content;
    private final boolean deleted;
    private final List<CommentResponse> replies = new ArrayList<>();

    private CommentResponse(Comment comment, AuthorSummary author) {
        boolean deleted = comment.isDeleted();
        this.id = comment.getId();
        this.authorId = deleted ? null : comment.getUserId();
        this.author = deleted ? null : author;
        this.content = deleted ? Comment.DELETED_CONTENT : comment.getContent();
        this.deleted = deleted;
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment, AuthorSummary.unknown());
    }

    public static CommentResponse from(Comment comment, AuthorSummary author) {
        return new CommentResponse(comment, author);
    }

    public void addReply(CommentResponse reply) {
        replies.add(reply);
    }
}
