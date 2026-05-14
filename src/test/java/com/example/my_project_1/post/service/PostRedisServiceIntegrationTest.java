package com.example.my_project_1.post.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostRedisServiceIntegrationTest {

    private static final String VIEW_DELTA_KEY = "post::view::delta::%s";
    private static final String VIEW_DIRTY_SET_KEY = "post::dirty::view";

    private LettuceConnectionFactory connectionFactory;
    private RedisTemplate<String, String> redisTemplate;
    private PostRedisService postRedisService;
    private Long postId;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 6379)
        );
        connectionFactory.afterPropertiesSet();

        try {
            connectionFactory.getConnection().ping();
        } catch (Exception e) {
            Assumptions.abort("localhost:6379 Redis is not available");
        }

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();

        postRedisService = new PostRedisService(redisTemplate);
        postId = System.nanoTime();
        cleanKeys(postId);
    }

    @AfterEach
    void tearDown() {
        if (redisTemplate != null && postId != null) {
            cleanKeys(postId);
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("실제 Redis increaseView는 view count와 view dirty marker를 함께 갱신한다.")
    void increaseView_incrementsViewCountAndMarksDirtyOnActualRedis() {
        postRedisService.increaseView(postId);

        assertThat(redisTemplate.opsForValue().get(viewDeltaKey(postId))).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 synced count와 Redis count가 같으면 dirty marker를 제거한다.")
    void acknowledgeSyncedViewDelta_removesDeltaAndDirtyWhenCurrentEqualsSyncedDelta() {
        postRedisService.increaseView(postId);

        boolean removed = postRedisService.acknowledgeSyncedViewDelta(postId, 1L);

        assertThat(removed).isTrue();
        assertThat(redisTemplate.opsForValue().get(viewDeltaKey(postId))).isNull();
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isFalse();
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 Redis count가 바뀌었으면 dirty marker를 유지한다.")
    void acknowledgeSyncedViewDelta_decrementsDeltaAndKeepsDirtyWhenCurrentIsGreater() {
        postRedisService.increaseView(postId);
        postRedisService.increaseView(postId);

        boolean removed = postRedisService.acknowledgeSyncedViewDelta(postId, 1L);

        assertThat(removed).isFalse();
        assertThat(redisTemplate.opsForValue().get(viewDeltaKey(postId))).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    private void cleanKeys(Long postId) {
        redisTemplate.delete(List.of(viewDeltaKey(postId)));
        redisTemplate.opsForSet().remove(VIEW_DIRTY_SET_KEY, postId.toString());
    }

    private String viewDeltaKey(Long postId) {
        return VIEW_DELTA_KEY.formatted(postId);
    }
}
