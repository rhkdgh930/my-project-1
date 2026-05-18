package com.example.my_project_1.post.controller;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.post.service.PostQueryService;
import com.example.my_project_1.post.service.response.PostListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tag Post API", description = "태그별 게시글 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags/{tagName}/posts")
public class TagPostController {

    private final PostQueryService postQueryService;

    @Operation(
            summary = "태그별 게시글 목록 조회",
            description = "태그명이 정확히 일치하는 활성 게시글을 최신순으로 조회합니다. 조회수는 증가하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "태그별 게시글 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class)))
    })
    @GetMapping
    public PageResponse<PostListResponse> getPostsByTagName(
            @PathVariable String tagName,
            @ParameterObject Pageable pageable
    ) {
        return postQueryService.getPostsByTagName(tagName, pageable);
    }
}
