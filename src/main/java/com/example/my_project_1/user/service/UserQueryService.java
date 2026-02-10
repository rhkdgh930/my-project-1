package com.example.my_project_1.user.service;

import com.example.my_project_1.user.domain.User;

public interface UserQueryService {
    User getByIdOrThrow(Long userId);
}
