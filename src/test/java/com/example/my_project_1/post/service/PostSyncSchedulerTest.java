package com.example.my_project_1.post.service;

import com.example.my_project_1.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostSyncSchedulerTest {

    private PostRedisService redisService;
    private PostRepository postRepository;
    private PostSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        redisService = mock(PostRedisService.class);
        postRepository = mock(PostRepository.class);
        scheduler = new PostSyncScheduler(redisService, postRepository);
    }

    @Test
    @DisplayName("sync는 view delta를 DB viewCount에 누적 반영한다.")
    void sync_addsViewDeltaToDbViewCount() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);

        scheduler.sync();

        verify(postRepository).updateViewCountDelta(postId, 100L);
        verify(postRepository, never()).updateCounts(anyLong(), anyLong(), anyLong());
        verify(redisService).acknowledgeSyncedViewDelta(postId, 100L);
        verify(redisService, never()).removeViewDirty(postId);
    }

    @Test
    @DisplayName("sync는 Redis view delta가 없으면 DB update 없이 dirty marker를 제거한다.")
    void sync_removesViewDirtyMarkerWhenRedisViewDeltaIsMissing() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(null);

        scheduler.sync();

        verify(postRepository, never()).updateViewCountDelta(anyLong(), anyLong());
        verify(redisService).removeViewDirty(postId);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sync는 Redis view delta가 0이면 DB update 없이 dirty marker를 제거한다.")
    void sync_removesViewDirtyMarkerWhenRedisViewDeltaIsZero() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(0L);

        scheduler.sync();

        verify(postRepository, never()).updateViewCountDelta(anyLong(), anyLong());
        verify(redisService).removeViewDirty(postId);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sync는 view DB 갱신 실패 시 Redis delta와 dirty marker를 유지한다.")
    void sync_keepsViewDeltaAndDirtyMarkerWhenViewDbUpdateFails() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);
        doThrow(new RuntimeException("db fail"))
                .when(postRepository).updateViewCountDelta(postId, 100L);

        scheduler.sync();

        verify(redisService, never()).removeViewDirty(postId);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
    }

    @Test
    @DisplayName("sync는 Redis acknowledge가 false여도 실패하지 않는다.")
    void sync_continuesWhenAcknowledgeSyncedViewDeltaReturnsFalse() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);
        when(redisService.acknowledgeSyncedViewDelta(postId, 100L)).thenReturn(false);

        scheduler.sync();

        verify(postRepository).updateViewCountDelta(postId, 100L);
        verify(redisService).acknowledgeSyncedViewDelta(postId, 100L);
    }
}
