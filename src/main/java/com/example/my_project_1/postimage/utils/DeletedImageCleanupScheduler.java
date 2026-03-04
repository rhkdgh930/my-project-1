package com.example.my_project_1.postimage.utils;

import com.example.my_project_1.postimage.domain.ImageStatus;
import com.example.my_project_1.postimage.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeletedImageCleanupScheduler {

    private final PostImageRepository postImageRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Transactional
    public void cleanup() {
        int deleted =
                postImageRepository.deleteByImageStatusAndCreatedAtBefore(
                        ImageStatus.DELETED,
                        LocalDateTime.now().minusDays(30)
                );

        log.info("[ImagePurge] deleted records = {}", deleted);
    }
}
