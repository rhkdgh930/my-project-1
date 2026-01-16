package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminQueryService;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class AdminQueryServiceImpl implements AdminQueryService {
    private final UserRepository userRepository;

    @Override
    public List<UserDetailResponse> findAll() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserDetailResponse::from).toList();
    }
}
