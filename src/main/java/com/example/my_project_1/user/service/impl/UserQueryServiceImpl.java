package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.UserAccountPolicy;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.UserQueryService;
import com.example.my_project_1.user.service.response.UserMeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;
    private final UserAccountPolicy userAccountPolicy;

    @Override
    public User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );
    }

    @Override
    public UserMeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        userAccountPolicy.validateMeReadable(user);

        return UserMeResponse.from(user);
    }
}
