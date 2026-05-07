package com.example.my_project_1.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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
    void increaseView_marksPostIdInViewDirtySet() {
        postRedisService.increaseView(10L);

        verify(valueOperations).increment("post::view::10");
        verify(setOperations).add("post::dirty::view", "10");
    }

    @Test
    @DisplayName("toggleLike는 Lua script가 liked 결과를 반환하면 true를 반환한다.")
    void toggleLike_returnsTrueWhenLuaScriptReturnsLikedResult() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(1L);

        boolean liked = postRedisService.toggleLike(10L, 100L);

        assertThat(liked).isTrue();
        verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("post::like::user::10", "post::like::10", "post::dirty::like")),
                eq("100"),
                eq("10")
        );
    }

    @Test
    @DisplayName("toggleLike는 Lua script가 unliked 결과를 반환하면 false를 반환한다.")
    void toggleLike_returnsFalseWhenLuaScriptReturnsUnlikedResult() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(0L);

        boolean liked = postRedisService.toggleLike(10L, 100L);

        assertThat(liked).isFalse();
    }

    @Test
    @DisplayName("toggleLike는 like dirty를 기록하고 음수 count를 방지하는 Lua script를 실행한다.")
    void toggleLike_executesLuaScriptThatMarksLikeDirtyAndPreventsNegativeCount() {
        ArgumentCaptor<RedisScript<Long>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.anyList(),
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
        )).thenReturn(1L);

        postRedisService.toggleLike(10L, 100L);

        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of("post::like::user::10", "post::like::10", "post::dirty::like")),
                eq("100"),
                eq("10")
        );
        String script = scriptCaptor.getValue().getScriptAsString();
        assertThat(script).contains("SISMEMBER", "SADD', KEYS[3]", "currentCount > 0");
        assertThat(script).contains("SET', KEYS[2], 0");
    }
}
