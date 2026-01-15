package com.example.my_project_1.user.service;

import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;

public interface UserService {
    UserSignUpResponse signUp(UserSignUpRequest request);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    UserWithdrawResponse withdraw(Long userId);
}
