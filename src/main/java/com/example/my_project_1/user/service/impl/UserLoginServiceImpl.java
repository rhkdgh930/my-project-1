package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.outbox.service.UserAccountChangeOutboxPublisher;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.UserAccountChangedType;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {
    private final Clock clock;
    private final UserRepository userRepository;
    private final UserAccountChangeOutboxPublisher userAccountChangeOutboxPublisher;

    @Transactional
    @Override
    public void processLogin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean dormantReleased = user.recordSuccessfulLogin(LocalDateTime.now(clock));

        if (dormantReleased) {
            userAccountChangeOutboxPublisher.publish(userId, UserAccountChangedType.DORMANT_RELEASED);
        }
    }
}
