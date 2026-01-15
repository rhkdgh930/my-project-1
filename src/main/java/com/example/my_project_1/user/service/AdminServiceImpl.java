package com.example.my_project_1.user.service;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final RedisUserContextService redisUserContextService;

    @Transactional
    @Override
    public void suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.suspend();

        redisUserContextService.evict(userId);
    }

    public List<UserDetailResponse> findAll() {
        List<User> users = userRepository.findAll();
        return users.stream().map(UserDetailResponse::from).toList();
    }
}
