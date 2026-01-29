package com.example.my_project_1.comment.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE comment SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Entity
@Table(name = "comment")
public class Comment extends BaseEntity {

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
    private Long parentId; // null이면 최상위 댓글

    @Column(nullable = false)
    private int depth; // 0 = 댓글, 1 = 대댓글

    /* ===== 생성 메서드 ===== */

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
        if (parent.depth >= 1) {
            throw new IllegalStateException("대댓글에는 답글을 달 수 없습니다.");
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
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.parentId = parentId;
        this.depth = depth;
    }

    /* ===== 도메인 로직 ===== */

    public void updateContent(String content, Long editorId) {
        validateAuthor(editorId);
        this.content = content;
    }

    public void delete(Long requesterId) {
        validateAuthor(requesterId);
        this.content = "삭제된 댓글입니다.";
        this.softDelete();
    }

    private void validateAuthor(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new IllegalStateException("작성자만 수정/삭제할 수 있습니다.");
        }
    }
}
