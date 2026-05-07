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
}
