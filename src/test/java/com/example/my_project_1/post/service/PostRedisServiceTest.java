package com.example.my_project_1.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

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
    @DisplayName("increaseView marks post id in view dirty set")
    void increaseView_marksPostIdInViewDirtySet() {
        postRedisService.increaseView(10L);

        verify(valueOperations).increment("post::view::10");
        verify(setOperations).add("post::dirty::view", "10");
    }

    @Test
    @DisplayName("toggleLike marks post id in like dirty set")
    void toggleLike_marksPostIdInLikeDirtySet() {
        when(setOperations.isMember("post::like::user::10", "100")).thenReturn(false);

        postRedisService.toggleLike(10L, 100L);

        verify(setOperations).add("post::like::user::10", "100");
        verify(valueOperations).increment("post::like::10");
        verify(setOperations).add("post::dirty::like", "10");
    }
}
