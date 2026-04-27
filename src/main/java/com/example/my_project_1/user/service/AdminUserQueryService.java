package com.example.my_project_1.user.service;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminUserQueryService {
    PageResponse<UserDetailResponse> findPage(Pageable pageable);
    List<UserDetailResponse> findNext(Long lastId, int size);
}
