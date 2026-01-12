package com.example.my_project_1.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void increaseView(Long postId) {
        redisTemplate.opsForValue().increment(viewKey(postId));
        redisTemplate.opsForSet().add("post:dirty", postId.toString());
    }

    public boolean toggleLike(Long postId, Long userId) {
        String key = likeUserKey(postId);
        boolean liked = Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(key, userId.toString())
        );

        if (liked) {
            redisTemplate.opsForSet().remove(key, userId.toString());
            redisTemplate.opsForValue().decrement(likeKey(postId));
        } else {
            redisTemplate.opsForSet().add(key, userId.toString());
            redisTemplate.opsForValue().increment(likeKey(postId));
        }

        redisTemplate.opsForSet().add("post:dirty", postId.toString());
        return !liked;
    }

    public long getView(Long postId) {
        String value = redisTemplate.opsForValue().get(viewKey(postId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public long getLike(Long postId) {
        String value = redisTemplate.opsForValue().get(likeKey(postId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public Set<String> getDirtyPostIds() {
        return redisTemplate.opsForSet().members("post:dirty");
    }

    public void clearDirtySet() {
        redisTemplate.delete("post:dirty");
    }

    private String viewKey(Long postId) {
        return "post:view:" + postId;
    }

    private String likeKey(Long postId) {
        return "post:like:" + postId;
    }

    private String likeUserKey(Long postId) {
        return "post:like:user:" + postId;
    }
}
