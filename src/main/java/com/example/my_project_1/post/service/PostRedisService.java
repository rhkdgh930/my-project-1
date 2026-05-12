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

    private static final String VIEW_KEY = "post::view::%s";
    private static final String VIEW_DIRTY_SET_KEY = "post::dirty::view";

    private static final Long REMOVED = 1L;

    private static final String INCREASE_VIEW_SCRIPT = """
            redis.call('INCR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """;

    private static final String REMOVE_DIRTY_IF_UNCHANGED_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
                redis.call('SREM', KEYS[2], ARGV[2])
                return 1
            end
            return 0
            """;

    public void increaseView(Long postId) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(INCREASE_VIEW_SCRIPT, Long.class);

        redisTemplate.execute(
                script,
                List.of(VIEW_KEY.formatted(postId), VIEW_DIRTY_SET_KEY),
                postId.toString()
        );
    }

    public long getView(Long postId) {
        Long value = getViewOrNull(postId);
        return value == null ? 0 : value;
    }

    public Long getViewOrNull(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_KEY.formatted(postId));
        return value == null ? null : Long.parseLong(value);
    }

    public Set<String> getViewDirtyPostIds() {
        return redisTemplate.opsForSet().members(VIEW_DIRTY_SET_KEY);
    }

    public void removeViewDirty(Long postId) {
        redisTemplate.opsForSet().remove(VIEW_DIRTY_SET_KEY, postId.toString());
    }

    public boolean removeViewDirtyIfUnchanged(Long postId, Long syncedCount) {
        return removeDirtyIfUnchanged(
                VIEW_KEY.formatted(postId),
                VIEW_DIRTY_SET_KEY,
                postId,
                syncedCount
        );
    }

    private boolean removeDirtyIfUnchanged(
            String countKey,
            String dirtySetKey,
            Long postId,
            Long syncedCount
    ) {
        if (syncedCount == null) {
            return false;
        }

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(REMOVE_DIRTY_IF_UNCHANGED_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(countKey, dirtySetKey),
                syncedCount.toString(),
                postId.toString()
        );

        return REMOVED.equals(result);
    }
}
