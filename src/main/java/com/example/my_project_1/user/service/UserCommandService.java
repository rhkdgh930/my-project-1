package com.example.my_project_1.user.service;

import com.example.my_project_1.user.service.request.*;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;

public interface UserCommandService {
    void sendVerificationCode(String emailValue);

    void verifyEmail(String email, String code);

    UserSignUpResponse signUp(UserSignUpRequest request);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    UserWithdrawResponse withdraw(Long userId, UserWithdrawRequest request);

    void cancelWithdraw(Long userId);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    void requestPasswordReset(String email);

    void resetPassword(PasswordResetRequest request);
}
