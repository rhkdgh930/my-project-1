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
    @DisplayName("sync updates only view count for view dirty post")
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
    @DisplayName("sync updates only like count for like dirty post")
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
    @DisplayName("sync keeps view dirty marker when redis view count is missing")
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
    @DisplayName("sync keeps like dirty marker when redis like count is missing")
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
    @DisplayName("sync keeps view dirty marker when view db update fails")
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
    @DisplayName("sync keeps like dirty marker when like db update fails")
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
