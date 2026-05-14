package com.example.my_project_1.image.service.impl;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageOwnerType;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.repository.ImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-06T01:02:03Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    @DisplayName("syncImages에 빈 목록을 전달하면 기존 POST 이미지를 DETACHED 처리한다.")
    void syncImages_detachesExistingPostImagesWhenNewKeysAreEmpty() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);
        Image image = Image.createPending(2L, "storage-key");
        image.attach(1L, ImageOwnerType.POST);

        when(imageRepository.findAllByOwnerIdAndOwnerType(1L, ImageOwnerType.POST))
                .thenReturn(List.of(image));

        imageService.syncImages(1L, ImageOwnerType.POST, List.of(), 2L);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.DETACHED);
        assertThat(image.getOwnerId()).isNull();
        assertThat(image.getOwnerType()).isNull();
        assertThat(image.getDetachedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("attachImages는 중복 storageKey를 distinct 처리한다.")
    void attachImages_distinctsDuplicateStorageKeys() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);
        Image image = Image.createPending(2L, "storage-key");

        when(imageRepository.findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L))
                .thenReturn(List.of(image));

        imageService.attachImages(1L, ImageOwnerType.POST, List.of("storage-key", "storage-key"), 2L);

        verify(imageRepository).findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L);
        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.USED);
        assertThat(image.getOwnerId()).isEqualTo(1L);
        assertThat(image.getOwnerType()).isEqualTo(ImageOwnerType.POST);
    }

    @Test
    @DisplayName("attachImages는 이미 같은 owner에 붙은 이미지를 재처리해도 성공한다.")
    void attachImages_isIdempotentWhenImageAlreadyAttachedToSameOwner() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);
        Image image = Image.createPending(2L, "storage-key");
        image.attach(1L, ImageOwnerType.POST);

        when(imageRepository.findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L))
                .thenReturn(List.of(image));

        imageService.attachImages(1L, ImageOwnerType.POST, List.of("storage-key"), 2L);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.USED);
        assertThat(image.getOwnerId()).isEqualTo(1L);
        assertThat(image.getOwnerType()).isEqualTo(ImageOwnerType.POST);
    }

    @Test
    @DisplayName("attachImages는 DETACHED 이미지를 다시 attach할 수 있다.")
    void attachImages_attachesDetachedImage() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);
        Image image = Image.createPending(2L, "storage-key");
        image.attach(1L, ImageOwnerType.POST);
        image.detach(LocalDateTime.now(CLOCK));

        when(imageRepository.findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L))
                .thenReturn(List.of(image));

        imageService.attachImages(1L, ImageOwnerType.POST, List.of("storage-key"), 2L);

        assertThat(image.getImageStatus()).isEqualTo(ImageStatus.USED);
        assertThat(image.getOwnerId()).isEqualTo(1L);
        assertThat(image.getOwnerType()).isEqualTo(ImageOwnerType.POST);
        assertThat(image.getDetachedAt()).isNull();
    }

    @Test
    @DisplayName("attachImages는 다른 owner에 이미 붙은 이미지를 거부한다.")
    void attachImages_rejectsImageAlreadyAttachedToDifferentOwner() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);
        Image image = Image.createPending(2L, "storage-key");
        image.attach(99L, ImageOwnerType.POST);

        when(imageRepository.findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L))
                .thenReturn(List.of(image));

        assertThatThrownBy(() -> imageService.attachImages(1L, ImageOwnerType.POST, List.of("storage-key"), 2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("attachImages는 storageKey 조회 결과가 부족하면 예외를 던진다.")
    void attachImages_rejectsMissingOrUnauthorizedStorageKeys() {
        ImageRepository imageRepository = mock(ImageRepository.class);
        ImageServiceImpl imageService = new ImageServiceImpl(imageRepository, CLOCK);

        when(imageRepository.findAllByStorageKeyInAndUploaderId(List.of("storage-key"), 2L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> imageService.attachImages(1L, ImageOwnerType.POST, List.of("storage-key"), 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
