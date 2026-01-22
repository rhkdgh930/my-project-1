package com.example.my_project_1.user.service;

import com.example.my_project_1.user.service.request.PasswordResetRequest;
import com.example.my_project_1.user.service.request.PasswordUpdateRequest;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;

public interface UserCommandService {
    UserSignUpResponse signUp(UserSignUpRequest request);

    void verifyEmail(String email, String code);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    UserWithdrawResponse withdraw(Long userId);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    void requestPasswordReset(String email);

    void resetPassword(PasswordResetRequest request);
}
