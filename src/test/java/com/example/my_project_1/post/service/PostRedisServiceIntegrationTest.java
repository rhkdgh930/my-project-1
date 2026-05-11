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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PostRedisServiceIntegrationTest {

    private static final String LIKE_USER_SET_KEY = "post::like::user::%s";
    private static final String VIEW_COUNT_KEY = "post::view::%s";
    private static final String LIKE_COUNT_KEY = "post::like::%s";
    private static final String VIEW_DIRTY_SET_KEY = "post::dirty::view";
    private static final String LIKE_DIRTY_SET_KEY = "post::dirty::like";

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
    @DisplayName("실제 Redis toggleLike는 좋아요와 좋아요 취소 시 count와 dirty marker를 갱신한다.")
    void toggleLike_likesAndUnlikesOnActualRedis() {
        Long userId = 100L;

        boolean liked = postRedisService.toggleLike(postId, userId);

        assertThat(liked).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(likeUserSetKey(postId), userId.toString()))
                .isTrue();
        assertThat(redisTemplate.opsForValue().get(likeCountKey(postId))).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(LIKE_DIRTY_SET_KEY, postId.toString()))
                .isTrue();

        boolean unliked = postRedisService.toggleLike(postId, userId);

        assertThat(unliked).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(likeUserSetKey(postId), userId.toString()))
                .isFalse();
        assertThat(redisTemplate.opsForValue().get(likeCountKey(postId))).isEqualTo("0");
    }

    @Test
    @DisplayName("실제 Redis increaseView는 view count와 view dirty marker를 함께 갱신한다.")
    void increaseView_incrementsViewCountAndMarksDirtyOnActualRedis() {
        postRedisService.increaseView(postId);

        assertThat(redisTemplate.opsForValue().get(viewCountKey(postId))).isEqualTo("1");
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 synced count와 Redis count가 같으면 dirty marker를 제거한다.")
    void removeViewDirtyIfUnchanged_removesDirtyWhenCountMatches() {
        postRedisService.increaseView(postId);

        boolean removed = postRedisService.removeViewDirtyIfUnchanged(postId, 1L);

        assertThat(removed).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isFalse();
    }

    @Test
    @DisplayName("removeViewDirtyIfUnchanged는 Redis count가 바뀌었으면 dirty marker를 유지한다.")
    void removeViewDirtyIfUnchanged_keepsDirtyWhenCountChanged() {
        postRedisService.increaseView(postId);
        postRedisService.increaseView(postId);

        boolean removed = postRedisService.removeViewDirtyIfUnchanged(postId, 1L);

        assertThat(removed).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(VIEW_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    @Test
    @DisplayName("removeLikeDirtyIfUnchanged는 synced count와 Redis count가 같으면 dirty marker를 제거한다.")
    void removeLikeDirtyIfUnchanged_removesDirtyWhenCountMatches() {
        postRedisService.toggleLike(postId, 100L);

        boolean removed = postRedisService.removeLikeDirtyIfUnchanged(postId, 1L);

        assertThat(removed).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(LIKE_DIRTY_SET_KEY, postId.toString()))
                .isFalse();
    }

    @Test
    @DisplayName("실제 Redis toggleLike는 count key가 없어도 count를 음수로 만들지 않는다.")
    void toggleLike_doesNotMakeCountNegativeWhenCountKeyIsMissing() {
        Long userId = 100L;
        redisTemplate.opsForSet().add(likeUserSetKey(postId), userId.toString());

        boolean unliked = postRedisService.toggleLike(postId, userId);

        assertThat(unliked).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(likeUserSetKey(postId), userId.toString()))
                .isFalse();
        assertThat(redisTemplate.opsForValue().get(likeCountKey(postId))).isEqualTo("0");
        assertThat(redisTemplate.opsForSet().isMember(LIKE_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    @Test
    @DisplayName("실제 Redis 동시 toggleLike는 membership과 count 정합성을 유지한다.")
    void toggleLike_keepsMembershipAndCountConsistentWhenConcurrentTogglesRun() throws Exception {
        Long userId = 100L;
        int toggleCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < toggleCount; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return postRedisService.toggleLike(postId, userId);
            }));
        }

        start.countDown();

        for (Future<Boolean> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(redisTemplate.opsForSet().isMember(likeUserSetKey(postId), userId.toString()))
                .isFalse();
        assertThat(redisTemplate.opsForValue().get(likeCountKey(postId))).isEqualTo("0");
        assertThat(redisTemplate.opsForSet().isMember(LIKE_DIRTY_SET_KEY, postId.toString()))
                .isTrue();
    }

    private void cleanKeys(Long postId) {
        redisTemplate.delete(List.of(viewCountKey(postId), likeUserSetKey(postId), likeCountKey(postId)));
        redisTemplate.opsForSet().remove(VIEW_DIRTY_SET_KEY, postId.toString());
        redisTemplate.opsForSet().remove(LIKE_DIRTY_SET_KEY, postId.toString());
    }

    private String likeUserSetKey(Long postId) {
        return LIKE_USER_SET_KEY.formatted(postId);
    }

    private String likeCountKey(Long postId) {
        return LIKE_COUNT_KEY.formatted(postId);
    }

    private String viewCountKey(Long postId) {
        return VIEW_COUNT_KEY.formatted(postId);
    }
}
