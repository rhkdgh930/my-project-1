package com.example.my_project_1.post.scheduler;

import com.example.my_project_1.post.service.PostRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostSyncScheduler {

    private final PostRedisService redisService;
    private final PostViewSyncWorker postViewSyncWorker;

    @Scheduled(fixedDelay = 10_000)
    public void sync() {
        syncViews();
    }

    private void syncViews() {
        Set<String> dirtyIds = redisService.getViewDirtyPostIds();
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        for (String id : dirtyIds) {
            try {
                Long postId = Long.parseLong(id);
                postViewSyncWorker.syncSingle(postId);
            } catch (Exception e) {
                log.error("[VIEW_SYNC_FAIL] postId={}", id, e);
            }
        }
    }
}