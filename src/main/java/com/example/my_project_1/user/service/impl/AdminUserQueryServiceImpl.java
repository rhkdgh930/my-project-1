package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.common.utils.PageResponse;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminUserQueryService;
import com.example.my_project_1.user.service.response.UserDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements AdminUserQueryService {
    private final UserRepository userRepository;


    @Override
    public PageResponse<UserDetailResponse> findPage(Pageable pageable) {
        return PageResponse.of(
                userRepository.findAll(pageable).map(UserDetailResponse::from)
        );
    }

    @Override
    public List<UserDetailResponse> findNext(Long lastId, int size) {
        return userRepository.findNextUsers(lastId, PageRequest.ofSize(size))
                .stream()
                .map(UserDetailResponse::from)
                .toList();
    }
}
