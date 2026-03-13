package com.example.my_project_1.image.utils;

import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {

        cleanByStatus(ImageStatus.PENDING, LocalDateTime.now().minusDays(1));
        cleanByStatus(ImageStatus.DETACHED, LocalDateTime.now().minusDays(7));

    }

    private void cleanByStatus(ImageStatus status, LocalDateTime threshold) {

        while (true) {

            List<Image> targets =
                    imageRepository.findTop100ByImageStatusAndCreatedAtBefore(status, threshold);

            if (targets.isEmpty()) break;

            for (Image image : targets) {

                imageStorage.delete(image.getStorageKey());

                image.markDeleted();

            }

        }

    }
}
