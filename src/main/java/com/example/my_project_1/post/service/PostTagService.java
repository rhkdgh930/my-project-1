package com.example.my_project_1.post.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.PostTag;
import com.example.my_project_1.post.domain.Tag;
import com.example.my_project_1.post.repository.PostTagNameProjection;
import com.example.my_project_1.post.repository.PostTagRepository;
import com.example.my_project_1.post.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostTagService {

    private static final int MAX_TAG_COUNT = 5;
    private static final int MAX_TAG_NAME_LENGTH = 20;

    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    public List<String> replaceTags(Long postId, Collection<String> tagNames) {
        List<String> normalizedNames = normalize(tagNames);

        postTagRepository.deleteByPostId(postId);

        if (normalizedNames.isEmpty()) {
            return List.of();
        }

        Map<String, Tag> tagMap = tagRepository.findByNameIn(normalizedNames).stream()
                .collect(Collectors.toMap(Tag::getName, tag -> tag));

        List<PostTag> postTags = new ArrayList<>();
        for (String name : normalizedNames) {
            Tag tag = tagMap.computeIfAbsent(name, key -> tagRepository.save(Tag.create(key)));
            postTags.add(PostTag.create(postId, tag.getId()));
        }

        postTagRepository.saveAll(postTags);
        return normalizedNames;
    }

    public Map<Long, List<String>> findTagNamesByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (PostTagNameProjection row : postTagRepository.findTagNamesByPostIdIn(postIds)) {
            result.computeIfAbsent(row.getPostId(), ignored -> new ArrayList<>())
                    .add(row.getName());
        }
        return result;
    }

    private List<String> normalize(Collection<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (String tagName : tagNames) {
            if (tagName == null) {
                continue;
            }

            String trimmed = tagName.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > MAX_TAG_NAME_LENGTH) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }

            normalized.putIfAbsent(trimmed, trimmed);
        }

        if (normalized.size() > MAX_TAG_COUNT) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return List.copyOf(normalized.values());
    }
}
