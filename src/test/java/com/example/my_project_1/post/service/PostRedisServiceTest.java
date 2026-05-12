package com.example.my_project_1.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostRedisServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;
    private PostRedisService postRedisService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        postRedisService = new PostRedisService(redisTemplate);
    }

    @Test
    @DisplayName("increaseView는 post id를 view dirty set에 기록한다.")
    void increaseView_executesLuaScriptWithViewCountAndDirtyKeys() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any()
        )).thenReturn(1L);

        postRedisService.increaseView(10L);

        verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("post::view::10", "post::dirty::view")),
                eq("10")
        );
        verify(valueOperations, never()).increment("post::view::10");
        verify(setOperations, never()).add("post::dirty::view", "10");
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 Lua result가 1이면 true를 반환한다.")
    void removeViewDirtyIfUnchanged_returnsTrueWhenLuaRemovesDirtyMarker() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(1L);

        boolean removed = postRedisService.removeViewDirtyIfUnchanged(10L, 100L);

        assertThat(removed).isTrue();
        verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("post::view::10", "post::dirty::view")),
                eq("100"),
                eq("10")
        );
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 Lua result가 0이면 false를 반환한다.")
    void removeViewDirtyIfUnchanged_returnsFalseWhenLuaKeepsDirtyMarker() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(0L);

        boolean removed = postRedisService.removeViewDirtyIfUnchanged(10L, 100L);

        assertThat(removed).isFalse();
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 syncedCount가 null이면 Redis를 호출하지 않고 false를 반환한다.")
    void removeViewDirtyIfUnchanged_returnsFalseWithoutRedisWhenSyncedCountIsNull() {
        boolean removed = postRedisService.removeViewDirtyIfUnchanged(10L, null);

        assertThat(removed).isFalse();
        verify(redisTemplate, never()).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        );
    }
}
