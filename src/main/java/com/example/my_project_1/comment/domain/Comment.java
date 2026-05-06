package com.example.my_project_1.comment.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comment")
public class Comment extends BaseEntity {
    public static final String DELETED_CONTENT = "삭제된 댓글입니다.";
    private static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private int depth;

    public static Comment createRoot(Long postId, Long userId, String content) {
        return Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .parentId(null)
                .depth(0)
                .build();
    }

    public static Comment createReply(Comment parent, Long userId, String content) {
        if (parent.isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
        if (parent.depth >= 1) {
            throw new CustomException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        return Comment.builder()
                .postId(parent.getPostId())
                .userId(userId)
                .content(content)
                .parentId(parent.getId())
                .depth(parent.getDepth() + 1)
                .build();
    }

    @Builder
    private Comment(Long postId, Long userId, String content, Long parentId, int depth) {
        notNull(postId, "게시판 정보는 필수입니다.");
        notNull(userId, "작성자 ID는 필수입니다.");
        validateContent(content);
        notNull(depth, "depth는 필수입니다.");

        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.parentId = parentId;
        this.depth = depth;
    }

    public void updateContent(String content, Long editorId) {
        validateAuthor(editorId);
        validateNotDeleted();
        validateContent(content);
        this.content = content;
    }

    public void delete(Long requesterId, LocalDateTime now) {
        validateAuthor(requesterId);
        if (isDeleted()) {
            return;
        }
        this.content = DELETED_CONTENT;
        super.softDelete(now);
    }

    private void validateAuthor(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateNotDeleted() {
        if (isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);
        }
    }

    private static void validateContent(String content) {
        hasText(content, "내용은 필수입니다.");
        isTrue(content.length() <= MAX_CONTENT_LENGTH, "내용은 1000자를 초과할 수 없습니다.");
    }
}
