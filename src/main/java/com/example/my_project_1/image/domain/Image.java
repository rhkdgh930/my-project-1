package com.example.my_project_1.image.domain;

import com.example.my_project_1.common.entity.BaseEntity;
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
@Table(name = "images")
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storageKey;

    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private ImageOwnerType ownerType;

    public static Image createPending(Long uploaderId, String storageKey) {
        return Image.builder()
                .uploaderId(uploaderId)
                .storageKey(storageKey)
                .status(ImageStatus.PENDING)
                .build();
    }

    public void attach(Long ownerId, ImageOwnerType ownerType) {
        if (!isAttachable()) {
            throw new IllegalStateException("이미 attach 할 수 없는 상태입니다.");
        }

        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.status = ImageStatus.USED;
    }

    public void detach() {
        if (this.status != ImageStatus.USED) {
            return;
        }

        this.ownerId = null;
        this.ownerType = null;
        this.status = ImageStatus.DETACHED;
    }

    public void markDeleted() {
        this.status = ImageStatus.DELETED;
    }

    public boolean isAttachable() {
        return this.status == ImageStatus.PENDING || this.status == ImageStatus.DETACHED;
    }

    @Builder
    private Image(String storageKey, Long uploaderId, ImageStatus status) {
        hasText(storageKey, "이미저 경로는 필수입니다.");
        notNull(uploaderId, "업로더명은 필수입니다.");
        notNull(status, "이미지 상태는 필수입니다.");
        this.storageKey = storageKey;
        this.uploaderId = uploaderId;
        this.status = status;
    }
}
