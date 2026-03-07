package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.*;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.EmailVerificationEvent;
import com.example.my_project_1.user.event.PasswordResetEvent;
import com.example.my_project_1.user.event.UserAccountChangedEvent;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.*;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final Clock clock;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisEmailVerificationService redisEmailVerificationService;
    private final RedisPasswordResetTokenService redisPasswordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void sendVerificationCode(String emailValue) {
        Email email = Email.from(emailValue);
        validateDuplicateEmail(email);

        eventPublisher.publishEvent(new EmailVerificationEvent(email.getValue()));
    }

    @Override
    public void verifyEmail(String email, String code) {
        redisEmailVerificationService.verifyCode(email, code);
    }

    @Override
    public UserSignUpResponse signUp(UserSignUpRequest request) {

        redisEmailVerificationService.checkIsVerified(request.getEmail());

        Email email = Email.from(request.getEmail());
        validateDuplicateEmail(email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.signUp(
                email,
                encodedPassword,
                request.getNickname(),
                LocalDateTime.now(clock)
        );
        User savedUser = userRepository.save(user);

        redisEmailVerificationService.deleteVerifiedStatus(request.getEmail());

        return UserSignUpResponse.from(savedUser);
    }

    private void validateDuplicateEmail(Email email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfile(
                request.getIntroduce(),
                request.getProfileImageUrl()
        );

        eventPublisher.publishEvent(UserAccountChangedEvent.profileUpdated(userId));

        return UserProfileResponse.from(user);
    }

    @Override
    public UserWithdrawResponse withdraw(Long userId, UserWithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.requestWithdrawal(LocalDateTime.now(clock));

        eventPublisher.publishEvent(UserAccountChangedEvent.securityStateChanged(userId));

        return UserWithdrawResponse.from(user);
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        eventPublisher.publishEvent(UserAccountChangedEvent.securityStateChanged(userId));
    }

    /***
     *아직 프론트 link 안 나옴
     */
    @Override
    public void requestPasswordReset(String emailValue) {
        Email email = Email.from(emailValue);

        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = redisPasswordResetTokenService.createAndSaveToken(emailValue);
            String link = frontendUrl + "/password-reset?token=" + rawToken;

            log.info("link = {}", link);
            eventPublisher.publishEvent(new PasswordResetEvent(emailValue, link));
        });
    }

    @Override
    public void resetPassword(PasswordResetRequest request) {
        String emailValue = redisPasswordResetTokenService.validateAndGetEmail(request.getToken());

        User user = userRepository.findByEmail(Email.from(emailValue))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        eventPublisher.publishEvent(UserAccountChangedEvent.securityStateChanged(user.getId()));

        redisPasswordResetTokenService.deleteToken(request.getToken());
    }
}
