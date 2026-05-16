package com.example.my_project_1.post.scheduler;

import com.example.my_project_1.common.monitoring.MonitoringService;
import com.example.my_project_1.post.repository.PostRepository;
import com.example.my_project_1.post.service.PostRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostViewSyncWorkerTest {

    private PostRedisService redisService;
    private PostRepository postRepository;
    private MonitoringService monitoringService;
    private PostViewSyncWorker worker;

    @BeforeEach
    void setUp() {
        redisService = mock(PostRedisService.class);
        postRepository = mock(PostRepository.class);
        monitoringService = mock(MonitoringService.class);
        worker = new PostViewSyncWorker(redisService, postRepository, monitoringService);
    }

    @Test
    @DisplayName("syncSingle은 view delta가 없으면 DB update 없이 dirty marker를 제거한다.")
    void syncSingle_removesDirtyWhenViewDeltaIsNull() {
        Long postId = 10L;
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(null);

        worker.syncSingle(postId);

        verify(postRepository, never()).updateViewCountDelta(anyLong(), anyLong());
        verify(redisService).removeViewDirty(postId);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
        verify(monitoringService).recordPostViewSyncSkipped("empty_delta");
    }

    @Test
    @DisplayName("syncSingle은 view delta가 0 이하이면 DB update 없이 dirty marker를 제거한다.")
    void syncSingle_removesDirtyWhenViewDeltaIsZeroOrNegative() {
        Long postId = 10L;
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(0L);

        worker.syncSingle(postId);

        verify(postRepository, never()).updateViewCountDelta(anyLong(), anyLong());
        verify(redisService).removeViewDirty(postId);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
        verify(monitoringService).recordPostViewSyncSkipped("empty_delta");
    }

    @Test
    @DisplayName("syncSingle은 view delta가 있으면 DB viewCount에 누적 반영하고 Redis delta를 ack한다.")
    void syncSingle_updatesViewCountAndAcknowledgesDelta() {
        Long postId = 10L;
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);
        when(postRepository.updateViewCountDelta(postId, 100L)).thenReturn(1);

        worker.syncSingle(postId);

        verify(postRepository).updateViewCountDelta(postId, 100L);
        verify(redisService).acknowledgeSyncedViewDelta(postId, 100L);
        verify(redisService, never()).removeViewDirty(postId);
        verify(monitoringService).recordPostViewSyncSuccess();
    }

    @Test
    @DisplayName("syncSingle은 DB update 대상이 없으면 Redis delta를 ack하지 않는다.")
    void syncSingle_doesNotAcknowledgeWhenDbUpdateAffectsNoRows() {
        Long postId = 10L;
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);
        when(postRepository.updateViewCountDelta(postId, 100L)).thenReturn(0);

        worker.syncSingle(postId);

        verify(postRepository).updateViewCountDelta(postId, 100L);
        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
        verify(redisService, never()).removeViewDirty(postId);
        verify(monitoringService).recordPostViewSyncFail("not_updated");
    }

    @Test
    @DisplayName("syncSingle은 DB update 중 예외가 발생하면 Redis delta를 ack하지 않는다.")
    void syncSingle_doesNotAcknowledgeWhenDbUpdateThrowsException() {
        Long postId = 10L;
        when(redisService.getViewDeltaOrNull(postId)).thenReturn(100L);
        doThrow(new RuntimeException("db fail"))
                .when(postRepository).updateViewCountDelta(postId, 100L);

        assertThatThrownBy(() -> worker.syncSingle(postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db fail");

        verify(redisService, never()).acknowledgeSyncedViewDelta(anyLong(), anyLong());
        verify(redisService, never()).removeViewDirty(postId);
        verify(monitoringService).recordPostViewSyncFail("exception");
    }
}
