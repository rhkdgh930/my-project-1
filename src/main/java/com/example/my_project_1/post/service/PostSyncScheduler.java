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

    @Scheduled(fixedDelay = 30_000) //30초
    @Transactional
    public void sync() {
        Set<String> dirtyIds = redisService.getDirtyPostIds();
        if (dirtyIds == null || dirtyIds.isEmpty()) return;

        for (String id : dirtyIds) {
            try {
                Long postId = Long.parseLong(id);

                postRepository.updateCounts(
                        postId,
                        redisService.getView(postId),
                        redisService.getLike(postId)
                );

                redisService.removeDirty(postId);

            } catch (Exception e) {
                log.error("[SYNC FAIL] postId={}", id, e);
            }
        }
    }
}