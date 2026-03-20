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

    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus imageStatus;

    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private ImageOwnerType ownerType;

    public static Image createPending(Long uploaderId, String storageKey) {
        return Image.builder()
                .uploaderId(uploaderId)
                .storageKey(storageKey)
                .imageStatus(ImageStatus.PENDING)
                .build();
    }

    public void attach(Long ownerId, ImageOwnerType ownerType) {

        if (isAlreadyAttachedTo(ownerId, ownerType)) {
            return;
        }

        if (this.ownerId != null && !this.ownerId.equals(ownerId)) {
            throw new IllegalStateException("이미 다른 엔티티에 연결된 이미지입니다.");
        }

        if (!isAttachable()) {
            throw new IllegalStateException("attach 불가능한 상태입니다. status=" + imageStatus);
        }

        this.ownerId = ownerId;
        this.ownerType = ownerType;
        this.imageStatus = ImageStatus.USED;
    }

    public void detach() {

        if (this.imageStatus == ImageStatus.DETACHED) {
            return;
        }

        if (this.imageStatus != ImageStatus.USED) {
            return;
        }

        this.ownerId = null;
        this.ownerType = null;
        this.imageStatus = ImageStatus.DETACHED;
    }

    public void markDeleted() {

        if (this.imageStatus == ImageStatus.DELETED) {
            return;
        }

        this.imageStatus = ImageStatus.DELETED;
    }

    public boolean isAttachable() {
        return this.imageStatus == ImageStatus.PENDING
                || this.imageStatus == ImageStatus.DETACHED;
    }

    private boolean isAlreadyAttachedTo(Long ownerId, ImageOwnerType ownerType) {
        return this.ownerId != null
                && this.ownerId.equals(ownerId)
                && this.ownerType == ownerType
                && this.imageStatus == ImageStatus.USED;
    }

    @Builder
    private Image(String storageKey, Long uploaderId, ImageStatus imageStatus) {
        hasText(storageKey, "이미지 경로는 필수입니다.");
        notNull(uploaderId, "업로더 ID는 필수입니다.");
        notNull(imageStatus, "이미지 상태는 필수입니다.");

        this.storageKey = storageKey;
        this.uploaderId = uploaderId;
        this.imageStatus = imageStatus;
    }
}