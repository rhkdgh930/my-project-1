package com.example.my_project_1.user.service;

import com.example.my_project_1.user.service.response.UserDetailResponse;
import com.example.my_project_1.user.service.response.UserProfileResponse;

import java.util.List;

public interface AdminService {
    void suspendUser(Long userId);
    List<UserDetailResponse> findAll();
}
