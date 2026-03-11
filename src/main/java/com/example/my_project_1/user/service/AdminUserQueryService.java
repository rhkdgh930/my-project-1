package com.example.my_project_1.user.service;

import com.example.my_project_1.user.service.response.UserDetailResponse;

import java.util.List;

public interface AdminUserQueryService {
    List<UserDetailResponse> findAll();
}
