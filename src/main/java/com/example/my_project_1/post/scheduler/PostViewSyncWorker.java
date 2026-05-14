package com.example.my_project_1.post.scheduler;

import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewSyncWorker {

    private final PostRedisService redisService;
    private final PostRepository postRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncSingle(Long postId) {
        Long viewDelta = redisService.getViewDeltaOrNull(postId);

        if (viewDelta == null || viewDelta <= 0) {
            redisService.removeViewDirty(postId);
            return;
        }

        int updated = postRepository.updateViewCountDelta(postId, viewDelta);

        if (updated == 0) {
            log.warn("[VIEW_SYNC][POST_NOT_FOUND_OR_NOT_UPDATED] postId={} delta={}", postId, viewDelta);
            return;
        }

        redisService.acknowledgeSyncedViewDelta(postId, viewDelta);
    }
}