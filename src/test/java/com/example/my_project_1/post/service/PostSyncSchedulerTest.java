package com.example.my_project_1.post.service;

import com.example.my_project_1.post.scheduler.PostSyncScheduler;
import com.example.my_project_1.post.scheduler.PostViewSyncWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostSyncSchedulerTest {

    private PostRedisService redisService;
    private PostViewSyncWorker postViewSyncWorker;
    private PostSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        redisService = mock(PostRedisService.class);
        postViewSyncWorker = mock(PostViewSyncWorker.class);
        scheduler = new PostSyncScheduler(redisService, postViewSyncWorker);
    }

    @Test
    @DisplayName("sync는 dirty id가 없으면 단건 동기화를 호출하지 않는다.")
    void sync_doesNothingWhenDirtyIdsAreEmpty() {
        when(redisService.getViewDirtyPostIds()).thenReturn(Set.of());

        scheduler.sync();

        verifyNoInteractions(postViewSyncWorker);
    }

    @Test
    @DisplayName("sync는 dirty id가 여러 개이면 각 postId를 단건 동기화 worker에 위임한다.")
    void sync_delegatesEachDirtyPostIdToWorker() {
        when(redisService.getViewDirtyPostIds()).thenReturn(linkedSet("10", "11"));

        scheduler.sync();

        verify(postViewSyncWorker).syncSingle(10L);
        verify(postViewSyncWorker).syncSingle(11L);
    }

    @Test
    @DisplayName("sync는 숫자가 아닌 dirty id가 있어도 나머지 postId 처리를 계속한다.")
    void sync_continuesWhenDirtyIdIsNotNumber() {
        when(redisService.getViewDirtyPostIds()).thenReturn(linkedSet("bad-id", "10"));

        scheduler.sync();

        verify(postViewSyncWorker, never()).syncSingle(null);
        verify(postViewSyncWorker).syncSingle(10L);
    }

    @Test
    @DisplayName("sync는 단건 동기화 실패가 있어도 나머지 postId 처리를 계속한다.")
    void sync_continuesWhenWorkerThrowsException() {
        when(redisService.getViewDirtyPostIds()).thenReturn(linkedSet("10", "11"));
        doThrow(new RuntimeException("db fail")).when(postViewSyncWorker).syncSingle(10L);

        scheduler.sync();

        verify(postViewSyncWorker).syncSingle(10L);
        verify(postViewSyncWorker).syncSingle(11L);
    }

    @Test
    @DisplayName("PostSyncScheduler는 전체 트랜잭션을 열지 않는다.")
    void scheduler_doesNotHaveTransactionalBoundary() throws NoSuchMethodException {
        Method sync = PostSyncScheduler.class.getDeclaredMethod("sync");

        assertThat(PostSyncScheduler.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(sync.isAnnotationPresent(Transactional.class)).isFalse();
    }

    private Set<String> linkedSet(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
