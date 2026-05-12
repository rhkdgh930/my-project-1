package com.example.my_project_1.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static org.springframework.util.Assert.notNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_like_post_user",
                columnNames = {"post_id", "user_id"}
        )
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostLike create(Post post, Long userId) {
        notNull(post, "post는 필수입니다.");
        notNull(userId, "userId는 필수입니다.");

        PostLike postLike = new PostLike();
        postLike.post = post;
        postLike.userId = userId;
        return postLike;
    }
}
