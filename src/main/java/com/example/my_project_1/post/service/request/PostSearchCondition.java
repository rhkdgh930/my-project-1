package com.example.my_project_1.post.service.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Schema(description = "게시글 검색/정렬 조건")
public class PostSearchCondition {

    @Schema(description = "검색어", example = "redis")
    private String keyword;

    @Schema(description = "검색 대상", example = "TITLE_CONTENT", defaultValue = "TITLE_CONTENT")
    private PostSearchType searchType = PostSearchType.TITLE_CONTENT;

    @Schema(description = "정렬 기준", example = "LATEST", defaultValue = "LATEST")
    private PostSortType sortType = PostSortType.LATEST;

    public boolean hasKeyword() {
        return StringUtils.hasText(keyword);
    }

    public String normalizedKeyword() {
        return hasKeyword() ? keyword.trim() : null;
    }

    public PostSearchType searchTypeOrDefault() {
        return searchType != null ? searchType : PostSearchType.TITLE_CONTENT;
    }

    public PostSortType sortTypeOrDefault() {
        return sortType != null ? sortType : PostSortType.LATEST;
    }
}