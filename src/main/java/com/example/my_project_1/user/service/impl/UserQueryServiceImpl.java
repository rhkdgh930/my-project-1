package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository userRepository;

    @Override
    public User getByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );
    }
}
