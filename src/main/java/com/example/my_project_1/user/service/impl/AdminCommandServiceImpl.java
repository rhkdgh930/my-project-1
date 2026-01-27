package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class AdminCommandServiceImpl implements AdminCommandService {
    private final UserRepository userRepository;
    private final RedisUserContextService redisUserContextService;
    private final RedisTokenService redisTokenService;

    @Override
    public void suspendUser(Long userId, SuspensionReason reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.suspend(reason);

        redisTokenService.deleteRefreshTokenHash(userId);
        redisUserContextService.evict(userId);
    }
}
