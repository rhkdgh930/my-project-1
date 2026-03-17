package com.example.my_project_1.user.batch;

import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawalCleanupJob {

    private static final int CHUNK_SIZE = 100;

    private final Clock clock;
    private final UserRepository userRepository;
    private final UserBatchProcessor processor;

    public void run() {

        StopWatch stopWatch = new StopWatch("WithdrawalCleanupBatch");
        stopWatch.start();

        log.info("[BATCH][WithdrawalCleanupJob][START]");

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusDays(7);

        Long lastId = 0L;

        int processedCount = 0;
        int failedChunkCount = 0;

        while (true) {

            List<Long> userIds =
                    userRepository.findWithdrawalUserIds(
                            lastId,
                            UserStatus.WITHDRAWN_REQUESTED,
                            threshold,
                            PageRequest.ofSize(CHUNK_SIZE)
                    );

            if (userIds.isEmpty()) {
                break;
            }

            try {

                processor.processWithdrawalChunk(userIds);

                processedCount += userIds.size();

            } catch (Exception e) {

                log.error(
                        "[BATCH][WithdrawalCleanupJob][CHUNK_FAIL] startUserId={} endUserId={} chunkSize={}",
                        userIds.get(0),
                        userIds.get(userIds.size() - 1),
                        userIds.size(),
                        e
                );

                failedChunkCount++;
            }

            lastId = userIds.get(userIds.size() - 1);
        }

        stopWatch.stop();

        log.info(
                "[BATCH][WithdrawalCleanupJob][COMPLETE] processed={} failedChunks={} elapsedMs={}",
                processedCount,
                failedChunkCount,
                stopWatch.getTotalTimeMillis()
        );
    }

}