package com.example.my_project_1.post.service;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.post.domain.PostTag;
import com.example.my_project_1.post.domain.Tag;
import com.example.my_project_1.post.repository.PostTagRepository;
import com.example.my_project_1.post.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostTagServiceTest {

    private TagRepository tagRepository;
    private PostTagRepository postTagRepository;
    private PostTagService postTagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        postTagRepository = mock(PostTagRepository.class);
        postTagService = new PostTagService(tagRepository, postTagRepository);
    }

    @Test
    @DisplayName("게시글 태그는 trim, 빈 값 제거, 중복 제거 후 저장한다.")
    void replaceTags_normalizesAndSavesTags() {
        Tag spring = tag(1L, "Spring");
        when(tagRepository.findByNameIn(List.of("Spring", "Redis"))).thenReturn(List.of(spring));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            ReflectionTestUtils.setField(tag, "id", 2L);
            return tag;
        });

        List<String> result = postTagService.replaceTags(
                10L,
                List.of(" Spring ", "", "Redis", "Spring", "   ")
        );

        assertThat(result).containsExactly("Spring", "Redis");
        verify(postTagRepository).deleteByPostId(10L);

        ArgumentCaptor<List<PostTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(postTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PostTag::getPostId).containsExactly(10L, 10L);
        assertThat(captor.getValue()).extracting(PostTag::getTagId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("태그가 null이면 빈 목록처럼 처리하고 기존 게시글 태그를 제거한다.")
    void replaceTags_treatsNullAsEmptyList() {
        List<String> result = postTagService.replaceTags(10L, null);

        assertThat(result).isEmpty();
        verify(postTagRepository).deleteByPostId(10L);
        verify(tagRepository, never()).findByNameIn(any());
        verify(postTagRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("게시글당 태그가 5개를 초과하면 실패한다.")
    void replaceTags_rejectsTooManyTags() {
        assertThatThrownBy(() -> postTagService.replaceTags(
                10L,
                List.of("a", "b", "c", "d", "e", "f")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postTagRepository, never()).deleteByPostId(10L);
    }

    @Test
    @DisplayName("태그명이 20자를 초과하면 실패한다.")
    void replaceTags_rejectsTooLongTagName() {
        assertThatThrownBy(() -> postTagService.replaceTags(10L, List.of("123456789012345678901")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(postTagRepository, never()).deleteByPostId(10L);
    }

    private Tag tag(Long id, String name) {
        Tag tag = Tag.create(name);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }
}
