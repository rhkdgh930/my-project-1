package com.example.my_project_1.postimage.utils;

import com.example.my_project_1.postimage.domain.ImageStatus;
import com.example.my_project_1.postimage.domain.Image;
import com.example.my_project_1.postimage.repository.PostImageRepository;
import com.example.my_project_1.postimage.service.ImageStorage;
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
public class OrphanImageCleanupScheduler {

    private final PostImageRepository postImageRepository;
    private final ImageStorage imageStorage;

    @Scheduled(cron = "0 0 * * * *") // 매 시간
    @Transactional
    public void cleanup() {
        List<Image> targets =
                postImageRepository.findTop100ByImageStatusAndCreatedAtBefore(
                        ImageStatus.PENDING,
                        LocalDateTime.now().minusHours(24)
                );

        for (Image image : targets) {
            imageStorage.delete(image.getImageUrl());
            image.markDeleted();
        }

        log.info("[ImageCleanup] cleaned = {}", targets.size());
    }
}
