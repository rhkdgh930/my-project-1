package com.example.my_project_1.postimage.domain;

import com.example.my_project_1.common.entity.BaseEntity;
import com.example.my_project_1.post.domain.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.notNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_image")
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, unique = true)
    private String imageUrl;

    @Column(nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus imageStatus;

    public static PostImage pending(Long uploaderId) {
        return PostImage.builder()
                .imageUrl("PENDING")
                .uploaderId(uploaderId)
                .imageStatus(ImageStatus.PENDING)
                .build();
    }

    public void uploaded(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void attach(Post post) {
        this.post = post;
        this.imageStatus = ImageStatus.USED;
    }

    public void detach() {
        this.post = null;
        this.imageStatus = ImageStatus.PENDING;
    }

    public void markDeleted() {
        this.imageStatus = ImageStatus.DELETED;
    }

    @Builder
    private PostImage(String imageUrl, Long uploaderId, ImageStatus imageStatus) {
        hasText(imageUrl, "이미저 경로는 필수입니다.");
        notNull(uploaderId, "업로더명은 필수입니다.");
        notNull(imageStatus, "이미지 상태는 필수입니다.");
        this.imageUrl = imageUrl;
        this.uploaderId = uploaderId;
        this.imageStatus = imageStatus;
    }
}
