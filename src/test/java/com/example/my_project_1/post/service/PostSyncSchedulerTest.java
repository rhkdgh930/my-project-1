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
    @DisplayName("sync는 view dirty post의 view count만 갱신한다.")
    void sync_updatesOnlyViewCountForViewDirtyPost() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getViewOrNull(postId)).thenReturn(100L);

        scheduler.sync();

        verify(postRepository).updateViewCount(postId, 100L);
        verify(postRepository, never()).updateLikeCount(anyLong(), anyLong());
        verify(postRepository, never()).updateCounts(anyLong(), anyLong(), anyLong());
        verify(redisService).removeViewDirty(postId);
        verify(redisService, never()).removeLikeDirty(postId);
    }

    @Test
    @DisplayName("sync는 like dirty post의 like count만 갱신한다.")
    void sync_updatesOnlyLikeCountForLikeDirtyPost() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeOrNull(postId)).thenReturn(5L);

        scheduler.sync();

        verify(postRepository, never()).updateViewCount(anyLong(), anyLong());
        verify(postRepository).updateLikeCount(postId, 5L);
        verify(postRepository, never()).updateCounts(anyLong(), anyLong(), anyLong());
        verify(redisService, never()).removeViewDirty(postId);
        verify(redisService).removeLikeDirty(postId);
    }

    @Test
    @DisplayName("sync는 redis view count가 없으면 view dirty marker를 유지한다.")
    void sync_keepsViewDirtyMarkerWhenRedisViewCountIsMissing() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getViewOrNull(postId)).thenReturn(null);

        scheduler.sync();

        verify(postRepository, never()).updateViewCount(anyLong(), anyLong());
        verify(redisService, never()).removeViewDirty(postId);
    }

    @Test
    @DisplayName("sync는 redis like count가 없으면 like dirty marker를 유지한다.")
    void sync_keepsLikeDirtyMarkerWhenRedisLikeCountIsMissing() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeOrNull(postId)).thenReturn(null);

        scheduler.sync();

        verify(postRepository, never()).updateLikeCount(anyLong(), anyLong());
        verify(redisService, never()).removeLikeDirty(postId);
    }

    @Test
    @DisplayName("sync는 view DB 갱신 실패 시 view dirty marker를 유지한다.")
    void sync_keepsViewDirtyMarkerWhenViewDbUpdateFails() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getViewOrNull(postId)).thenReturn(100L);
        doThrow(new RuntimeException("db fail"))
                .when(postRepository).updateViewCount(postId, 100L);

        scheduler.sync();

        verify(redisService, never()).removeViewDirty(postId);
    }

    @Test
    @DisplayName("sync는 like DB 갱신 실패 시 like dirty marker를 유지한다.")
    void sync_keepsLikeDirtyMarkerWhenLikeDbUpdateFails() {
        Long postId = 10L;
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of());
        when(redisService.getLikeDirtyPostIds()).thenReturn(Set.of(postId.toString()));
        when(redisService.getLikeOrNull(postId)).thenReturn(5L);
        doThrow(new RuntimeException("db fail"))
                .when(postRepository).updateLikeCount(postId, 5L);

        scheduler.sync();

        verify(redisService, never()).removeLikeDirty(postId);
    }
}
