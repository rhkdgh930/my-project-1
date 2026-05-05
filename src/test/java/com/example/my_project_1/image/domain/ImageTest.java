package com.example.my_project_1.image.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {

    @Test
    @DisplayName("detach는 detachedAt을 기록한다.")
    void detach_recordsDetachedAt() {
        Image image = Image.createPending(1L, "storage-key");
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 10, 0);
        image.attach(10L, ImageOwnerType.POST);

        image.detach(now);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.DETACHED);
        assertThat(image.getOwnerId()).isNull();
        assertThat(image.getOwnerType()).isNull();
        assertThat(image.getDetachedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("attach는 detached 이미지를 재사용할 때 detachedAt을 초기화한다.")
    void attach_clearsDetachedAtWhenDetachedImageIsReused() {
        Image image = Image.createPending(1L, "storage-key");
        image.attach(10L, ImageOwnerType.POST);
        image.detach(LocalDateTime.of(2026, 5, 5, 10, 0));

        image.attach(11L, ImageOwnerType.POST);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.USED);
        assertThat(image.getOwnerId()).isEqualTo(11L);
        assertThat(image.getDetachedAt()).isNull();
    }

    @Test
    @DisplayName("markDeleted는 deletedAt을 기록한다.")
    void markDeleted_recordsDeletedAt() {
        Image image = Image.createPending(1L, "storage-key");
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 10, 0);

        image.markDeleted(now);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.DELETED);
        assertThat(image.getDeletedAt()).isEqualTo(now);
    }
}
