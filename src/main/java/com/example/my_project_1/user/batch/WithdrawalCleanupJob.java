package com.example.my_project_1.user.batch;

import com.example.my_project_1.user.domain.User;
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

            List<User> users =
                    userRepository.findWithdrawalUsers(
                            lastId,
                            UserStatus.WITHDRAWN_REQUESTED,
                            threshold,
                            PageRequest.ofSize(CHUNK_SIZE)
                    );

            if (users.isEmpty()) break;

            try {

                processor.processWithdrawalChunk(users);

                processedCount += users.size();

            } catch (Exception e) {
                failedChunkCount++;
            }

            lastId = users.get(users.size() - 1).getId();
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