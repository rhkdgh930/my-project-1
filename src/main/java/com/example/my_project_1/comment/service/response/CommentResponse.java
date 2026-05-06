package com.example.my_project_1.comment.service.response;

import com.example.my_project_1.comment.domain.Comment;
import com.example.my_project_1.user.client.AuthorSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Schema(description = "댓글/대댓글 응답. 삭제된 댓글은 tombstone 형태로 반환됩니다.")
public class CommentResponse {
    @Schema(description = "댓글 ID", example = "1")
    private final Long id;

    @Schema(description = "전환 기간 유지 필드. 삭제된 댓글에서는 null입니다.", example = "1", nullable = true)
    private final Long authorId;

    @Schema(description = "작성자 표시 정보. 삭제된 댓글에서는 null입니다.", nullable = true)
    private final AuthorSummary author;

    @Schema(description = "댓글 내용. 삭제된 댓글에서는 tombstone 문구입니다.", example = "댓글 내용입니다.")
    private final String content;

    @Schema(description = "삭제 여부", example = "false")
    private final boolean deleted;

    @Schema(description = "대댓글 목록")
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
