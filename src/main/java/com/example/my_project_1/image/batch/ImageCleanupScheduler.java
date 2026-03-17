package com.example.my_project_1.image.batch;

import com.example.my_project_1.common.logging.BatchTraceHelper;
import com.example.my_project_1.image.domain.ImageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ImageCleanupJob imageCleanupJob;
    private final Clock clock;

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        BatchTraceHelper.start();

        try {
            LocalDateTime now = LocalDateTime.now(clock);
            log.info("[BATCH][ImageCleanup][START]");

            imageCleanupJob.cleanup(ImageStatus.PENDING, now.minusDays(1));
            imageCleanupJob.cleanup(ImageStatus.DETACHED, now.minusDays(7));

            log.info("[BATCH][ImageCleanup][FINISH]");
        } finally {
            BatchTraceHelper.clear();
        }
    }
}