package com.example.my_project_1.user.utils;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.event.DormancyNotifyEvent;
import com.example.my_project_1.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormantBatchJob {

    private static final int CHUNK_SIZE = 100; // 한 번에 처리할 트랜잭션 크기
    private final UserRepository userRepository;
    private final UserBatchProcessor userBatchProcessor;

    // [Note] Job 자체에는 트랜잭션을 걸지 않아 DB Connection을 오래 잡지 않도록 함
    @Scheduled(cron = "0 0 3 * * *")
    public void processDormancy() {
        StopWatch stopWatch = new StopWatch("DormantBatch");
        stopWatch.start();
        log.info("[DormantBatch] Started.");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime notifyThreshold = now.minusMonths(11);
        LocalDateTime dormantThreshold = now.minusMonths(12);

        Long lastId = 0L;
        int processedCount = 0;

        while (true) {
            // [Step 1] 처리 대상 ID 조회 (DB 부하 최소화)
            // Keyset Pagination: offset 없이 lastId 기준으로 인덱스 스캔
            Slice<Long> idSlice = userRepository.findDormantCandidateIds(
                    lastId,
                    UserStatus.ACTIVE,
                    notifyThreshold,
                    PageRequest.ofSize(CHUNK_SIZE)
            );

            if (idSlice.isEmpty()) break;

            List<Long> userIds = idSlice.getContent();

            // [Step 2] Chunk 단위 트랜잭션 실행 (위임)
            userBatchProcessor.processChunk(userIds, dormantThreshold);

            processedCount += userIds.size();

            // 다음 Keyset을 위해 마지막 ID 갱신
            lastId = userIds.get(userIds.size() - 1);

            // 더 이상 페이지가 없으면 종료
            if (!idSlice.hasNext()) break;
        }

        stopWatch.stop();
        log.info("[DormantBatch] Completed. Total Processed: {}, Elapsed: {}ms",
                processedCount, stopWatch.getTotalTimeMillis());
    }
}

