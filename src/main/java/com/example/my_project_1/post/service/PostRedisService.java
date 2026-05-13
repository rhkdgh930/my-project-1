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

    private static final String VIEW_DELTA_KEY = "post::view::delta::%s";
    private static final String VIEW_DIRTY_SET_KEY = "post::dirty::view";

    private static final Long REMOVED = 1L;

    private static final String INCREASE_VIEW_SCRIPT = """
            redis.call('INCR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1
            """;

    private static final String ACK_VIEW_DELTA_SCRIPT = """
            local current = redis.call('GET', KEYS[1])
            if not current then
                redis.call('SREM', KEYS[2], ARGV[2])
                return 1
            end

            current = tonumber(current)
            local synced = tonumber(ARGV[1])

            if current <= 0 then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[2], ARGV[2])
                return 1
            end

            if current == synced then
                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[2], ARGV[2])
                return 1
            end

            if current > synced then
                redis.call('DECRBY', KEYS[1], synced)
                return 0
            end

            return 0
            """;

    public void increaseView(Long postId) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(INCREASE_VIEW_SCRIPT, Long.class);

        redisTemplate.execute(
                script,
                List.of(VIEW_DELTA_KEY.formatted(postId), VIEW_DIRTY_SET_KEY),
                postId.toString()
        );
    }

    public long getViewDelta(Long postId) {
        Long value = getViewDeltaOrNull(postId);
        return value == null ? 0 : value;
    }

    public Long getViewDeltaOrNull(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_DELTA_KEY.formatted(postId));
        return value == null ? null : Long.parseLong(value);
    }

    public Set<String> getViewDirtyPostIds() {
        return redisTemplate.opsForSet().members(VIEW_DIRTY_SET_KEY);
    }

    public void removeViewDirty(Long postId) {
        redisTemplate.opsForSet().remove(VIEW_DIRTY_SET_KEY, postId.toString());
    }

    public boolean acknowledgeSyncedViewDelta(Long postId, Long syncedDelta) {
        return acknowledgeSyncedDelta(
                VIEW_DELTA_KEY.formatted(postId),
                VIEW_DIRTY_SET_KEY,
                postId,
                syncedDelta
        );
    }

    private boolean acknowledgeSyncedDelta(
            String deltaKey,
            String dirtySetKey,
            Long postId,
            Long syncedDelta
    ) {
        if (syncedDelta == null || syncedDelta <= 0) {
            return false;
        }

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(ACK_VIEW_DELTA_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(deltaKey, dirtySetKey),
                syncedDelta.toString(),
                postId.toString()
        );

        return REMOVED.equals(result);
    }
}
