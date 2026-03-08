package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.UserAccountChangedEvent;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.AdminCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Transactional
@Service
@RequiredArgsConstructor
public class AdminCommandServiceImpl implements AdminCommandService {
    private final Clock clock;

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void suspendUser(Long userId, SuspensionType type, SuspensionReason reason, Duration duration) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.suspend(type, reason, duration, LocalDateTime.now(clock));

        eventPublisher.publishEvent(UserAccountChangedEvent.securityStateChanged(userId));
    }
}
