package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminUserQueryService;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {
    private final UserRepository userRepository;

    @Override
    public List<UserDetailResponse> findAll() {
        List<User> users = userRepository.findAllRaw();
        return users.stream().map(UserDetailResponse::from).toList();
    }
}
