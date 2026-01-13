package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;

public interface UserService {
    UserSignUpResponse signUp(UserSignUpRequest request);

    UserDetailResponse updateProfile(Long userId, UserProfileUpdateRequest request);
}
