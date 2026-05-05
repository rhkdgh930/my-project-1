package com.example.my_project_1.image.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {

    @Test
    @DisplayName("detach records detachedAt")
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
    @DisplayName("attach clears detachedAt when detached image is reused")
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
    @DisplayName("markDeleted records deletedAt")
    void markDeleted_recordsDeletedAt() {
        Image image = Image.createPending(1L, "storage-key");
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 10, 0);

        image.markDeleted(now);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.DELETED);
        assertThat(image.getDeletedAt()).isEqualTo(now);
    }
}
