package com.example.my_project_1.image.batch;

import com.example.my_project_1.image.domain.Image;
import com.example.my_project_1.image.domain.ImageStatus;
import com.example.my_project_1.image.repository.ImageRepository;
import com.example.my_project_1.image.service.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupJob {

    private static final int CHUNK_SIZE = 100;

    private final ImageRepository imageRepository;
    private final ImageStorage imageStorage;
    private final ImageBatchProcessor processor;

    public void cleanup(ImageStatus status, LocalDateTime threshold) {

        log.info("[BATCH][ImageCleanup][START] status={} threshold={}", status, threshold);

        Long lastId = 0L;
        int processedCount = 0;
        int failCount = 0;

        while (true) {

            List<Image> images = imageRepository.findCleanupTargets(
                    lastId,
                    status,
                    threshold,
                    PageRequest.ofSize(CHUNK_SIZE)
            );

            if (images.isEmpty()) break;

            List<Long> successIds = new ArrayList<>();

            for (Image image : images) {
                try {
                    imageStorage.delete(image.getStorageKey());
                    successIds.add(image.getId());
                } catch (Exception e) {
                    failCount++;
                    log.error(
                            "[BATCH][ImageCleanup][DELETE_FAIL] imageId={} key={}",
                            image.getId(),
                            image.getStorageKey(),
                            e
                    );
                }
            }

            processor.markDeletedBulk(successIds);

            processedCount += successIds.size();
            lastId = images.get(images.size() - 1).getId();
        }

        log.info(
                "[BATCH][ImageCleanup][COMPLETE] status={} processed={} fail={}",
                status,
                processedCount,
                failCount
        );
    }
}