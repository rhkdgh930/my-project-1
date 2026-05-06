package com.example.my_project_1.common.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Spring Page 기반 공통 페이지 응답")
public class PageResponse<T> {
    @Schema(description = "현재 페이지 데이터 목록")
    private List<T> content;

    @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0")
    private int pageNumber;

    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;

    @Schema(description = "전체 데이터 수", example = "134")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "7")
    private int totalPages;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
