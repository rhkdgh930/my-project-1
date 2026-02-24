package com.example.my_project_1.user.utils;

import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawalCleanupJob {

    private static final int CHUNK_SIZE = 100;
    private final UserRepository userRepository;
    private final UserBatchProcessor userBatchProcessor;

    // FACT: 기존에 있던 @Transactional을 반드시 제거해야 함
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupWithdrawnUsers() {
        log.info("[WithdrawalCleanupBatch] Started.");

        // UserWithdrawal.RETENTION_DAYS (7일) 기준
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        Long lastId = 0L;
        int processedCount = 0;

        while (true) {
            Slice<Long> idSlice = userRepository.findWithdrawalTargetIds(
                    lastId,
                    UserStatus.WITHDRAWN_REQUESTED,
                    threshold,
                    PageRequest.ofSize(CHUNK_SIZE)
            );

            if (idSlice.isEmpty()) break;

            List<Long> userIds = idSlice.getContent();

            // 트랜잭션 분리 호출
            userBatchProcessor.processWithdrawalChunk(userIds);

            processedCount += userIds.size();
            lastId = userIds.get(userIds.size() - 1);

            if (!idSlice.hasNext()) break;
        }

        log.info("[WithdrawalCleanupBatch] Completed. Total Processed: {}", processedCount);
    }
}