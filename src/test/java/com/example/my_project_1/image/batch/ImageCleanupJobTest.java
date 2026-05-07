package com.example.my_project_1.image.batch;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageCleanupJobTest {

    private final ImageRepository imageRepository = mock(ImageRepository.class);
    private final ImageStorage imageStorage = mock(ImageStorage.class);
    private final ImageBatchProcessor processor = mock(ImageBatchProcessor.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-05-05T01:00:00Z"),
            ZoneId.of("UTC")
    );
    private final ImageCleanupJob imageCleanupJob =
            new ImageCleanupJob(imageRepository, imageStorage, processor, clock);

    @Test
    @DisplayName("cleanup은 파일 삭제에 성공한 이미지만 deleted 처리한다.")
    void cleanup_marksOnlySuccessfullyDeletedImagesAsDeleted() {
        LocalDateTime threshold = LocalDateTime.of(2026, 5, 4, 1, 0);
        Image success = image(1L, "success-key");
        Image failure = image(2L, "failure-key");
        when(imageRepository.findPendingCleanupTargets(
                eq(0L),
                eq(threshold),
                eq(PageRequest.ofSize(100))
        )).thenReturn(List.of(success, failure));
        when(imageRepository.findPendingCleanupTargets(
                eq(2L),
                eq(threshold),
                eq(PageRequest.ofSize(100))
        )).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("delete fail"))
                .when(imageStorage).delete("failure-key");

        imageCleanupJob.cleanup(ImageStatus.PENDING, threshold);

        verify(imageStorage).delete("success-key");
        verify(imageStorage).delete("failure-key");
        verify(processor).markDeletedBulk(
                eq(List.of(1L)),
                eq(LocalDateTime.ofInstant(clock.instant(), clock.getZone()))
        );
    }

    @Test
    @DisplayName("detached cleanup은 detached cleanup query를 사용한다.")
    void cleanup_usesDetachedCleanupQueryWhenStatusIsDetached() {
        LocalDateTime threshold = LocalDateTime.of(2026, 5, 4, 1, 0);
        when(imageRepository.findDetachedCleanupTargets(
                eq(0L),
                eq(threshold),
                eq(PageRequest.ofSize(100))
        )).thenReturn(List.of());

        imageCleanupJob.cleanup(ImageStatus.DETACHED, threshold);

        verify(imageRepository).findDetachedCleanupTargets(
                eq(0L),
                eq(threshold),
                eq(PageRequest.ofSize(100))
        );
    }

    private Image image(Long id, String storageKey) {
        Image image = Image.createPending(1L, storageKey);
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
