package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.service.response.UserMeResponse;

public interface UserQueryService {
    User getByIdOrThrow(Long userId);

    UserMeResponse getMe(Long userId);
}
