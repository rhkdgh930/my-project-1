package com.example.my_project_1.post.service;

import com.example.my_project_1.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostSyncScheduler {

    private final PostRedisService redisService;
    private final PostRepository postRepository;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
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
                Long viewCount = redisService.getViewOrNull(postId);

                if (viewCount == null) {
                    continue;
                }

                postRepository.updateViewCount(postId, viewCount);
                redisService.removeViewDirtyIfUnchanged(postId, viewCount);

            } catch (Exception e) {
                log.error("[VIEW_SYNC_FAIL] postId={}", id, e);
            }
        }
    }

}
