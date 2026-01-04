package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.service.request.UserSignUpRequest;

public interface UserService {
    User signUp(UserSignUpRequest request);
    void suspendUser(Long userId);

}
