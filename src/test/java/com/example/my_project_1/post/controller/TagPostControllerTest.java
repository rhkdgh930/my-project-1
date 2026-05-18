package com.example.my_project_1.post.controller;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.response.PostListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagPostControllerTest {

    private final PostQueryService postQueryService = mock(PostQueryService.class);
    private final TagPostController tagPostController = new TagPostController(postQueryService);

    @Test
    @DisplayName("태그별 게시글 목록 조회 요청을 서비스로 위임한다.")
    void getPostsByTagName_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<PostListResponse> response = PageResponse.of(new PageImpl<>(List.of(), pageable, 0));
        when(postQueryService.getPostsByTagName("Spring", pageable)).thenReturn(response);

        PageResponse<PostListResponse> actual = tagPostController.getPostsByTagName("Spring", pageable);

        assertThat(actual).isSameAs(response);
        verify(postQueryService).getPostsByTagName("Spring", pageable);
    }
}
