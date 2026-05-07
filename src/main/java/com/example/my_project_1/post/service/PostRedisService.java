package com.example.my_project_1.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 키 컨벤션: post::[기능]::[식별자]
    private static final String VIEW_KEY = "post::view::%s";
    private static final String LIKE_CNT_KEY = "post::like::%s";
    private static final String LIKE_USER_SET_KEY = "post::like::user::%s";
    private static final String DIRTY_SET_KEY = "post::dirty";
    private static final String VIEW_DIRTY_SET_KEY = "post::dirty::view";
    private static final String LIKE_DIRTY_SET_KEY = "post::dirty::like";
    private static final Long LIKED = 1L;
    private static final String TOGGLE_LIKE_SCRIPT = """
            local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1])
            if isMember == 1 then
                redis.call('SREM', KEYS[1], ARGV[1])
                local currentCount = tonumber(redis.call('GET', KEYS[2]) or '0')
                if currentCount > 0 then
                    redis.call('DECR', KEYS[2])
                else
                    redis.call('SET', KEYS[2], 0)
                end
                redis.call('SADD', KEYS[3], ARGV[2])
                return 0
            end
            redis.call('SADD', KEYS[1], ARGV[1])
            redis.call('INCR', KEYS[2])
            redis.call('SADD', KEYS[3], ARGV[2])
            return 1
            """;

    public void increaseView(Long postId) {
        redisTemplate.opsForValue().increment(VIEW_KEY.formatted(postId));
        markViewAsDirty(postId);
    }

    public boolean toggleLike(Long postId, Long userId) {
        String userSetKey = LIKE_USER_SET_KEY.formatted(postId);
        String likeCntKey = LIKE_CNT_KEY.formatted(postId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOGGLE_LIKE_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(userSetKey, likeCntKey, LIKE_DIRTY_SET_KEY),
                userId.toString(),
                postId.toString()
        );

        return LIKED.equals(result);
    }

    public long getView(Long postId) {
        Long value = getViewOrNull(postId);
        return value == null ? 0 : value;
    }

    public long getLike(Long postId) {
        Long value = getLikeOrNull(postId);
        return value == null ? 0 : value;
    }

    public Long getViewOrNull(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_KEY.formatted(postId));
        return value == null ? null : Long.parseLong(value);
    }

    public Long getLikeOrNull(Long postId) {
        String value = redisTemplate.opsForValue().get(LIKE_CNT_KEY.formatted(postId));
        return value == null ? null : Long.parseLong(value);
    }

    public Set<String> getDirtyPostIds() {
        return redisTemplate.opsForSet().members(DIRTY_SET_KEY);
    }

    public Set<String> getViewDirtyPostIds() {
        return redisTemplate.opsForSet().members(VIEW_DIRTY_SET_KEY);
    }

    public Set<String> getLikeDirtyPostIds() {
        return redisTemplate.opsForSet().members(LIKE_DIRTY_SET_KEY);
    }

    public void clearDirtySet() {
        redisTemplate.delete(DIRTY_SET_KEY);
    }

    public void removeDirty(Long postId) {
        redisTemplate.opsForSet().remove(DIRTY_SET_KEY, postId.toString());
    }

    public void removeViewDirty(Long postId) {
        redisTemplate.opsForSet().remove(VIEW_DIRTY_SET_KEY, postId.toString());
    }

    public void removeLikeDirty(Long postId) {
        redisTemplate.opsForSet().remove(LIKE_DIRTY_SET_KEY, postId.toString());
    }

    private void markViewAsDirty(Long postId) {
        redisTemplate.opsForSet().add(VIEW_DIRTY_SET_KEY, postId.toString());
    }

    private void markLikeAsDirty(Long postId) {
        redisTemplate.opsForSet().add(LIKE_DIRTY_SET_KEY, postId.toString());
    }
}
