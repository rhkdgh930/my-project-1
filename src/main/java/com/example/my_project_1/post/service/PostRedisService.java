package com.example.my_project_1.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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

    public void increaseView(Long postId) {
        redisTemplate.opsForValue().increment(VIEW_KEY.formatted(postId));
        markAsDirty(postId);
    }

    public boolean toggleLike(Long postId, Long userId) {
        String userSetKey = LIKE_USER_SET_KEY.formatted(postId);
        String likeCntKey = LIKE_CNT_KEY.formatted(postId);

        boolean isMember = Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(userSetKey, userId.toString())
        );

        if (isMember) {
            redisTemplate.opsForSet().remove(userSetKey, userId.toString());
            redisTemplate.opsForValue().decrement(likeCntKey);
        } else {
            redisTemplate.opsForSet().add(userSetKey, userId.toString());
            redisTemplate.opsForValue().increment(likeCntKey);
        }

        markAsDirty(postId);
        return !isMember;
    }

    public long getView(Long postId) {
        String value = redisTemplate.opsForValue().get(VIEW_KEY.formatted(postId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public long getLike(Long postId) {
        String value = redisTemplate.opsForValue().get(LIKE_CNT_KEY.formatted(postId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public Set<String> getDirtyPostIds() {
        return redisTemplate.opsForSet().members(DIRTY_SET_KEY);
    }

    public void clearDirtySet() {
        redisTemplate.delete(DIRTY_SET_KEY);
    }

    private void markAsDirty(Long postId) {
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, postId.toString());
    }
}
