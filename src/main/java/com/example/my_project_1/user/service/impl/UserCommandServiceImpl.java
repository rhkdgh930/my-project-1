package com.example.my_project_1.user.service.impl;

import com.example.my_project_1.auth.service.*;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.event.EmailVerificationEvent;
import com.example.my_project_1.user.event.PasswordResetEvent;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.UserCommandService;
import com.example.my_project_1.user.service.request.PasswordResetRequest;
import com.example.my_project_1.user.service.request.PasswordUpdateRequest;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUserContextService redisUserContextService;
    private final RedisTokenService redisTokenService;
    private final EmailService emailService;
    private final RedisEmailVerificationService redisEmailVerificationService;
    private final RedisPasswordResetTokenService redisPasswordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void sendVerificationCode(String emailValue) {
        Email email = Email.from(emailValue);
        validateDuplicateEmail(email); // 이미 가입된 이메일인지 체크

        // 이메일 발송 (비동기 이벤트 적용하신 경우 eventPublisher 사용)
        eventPublisher.publishEvent(new EmailVerificationEvent(email.getValue()));
    }

    // 2. 인증 코드 검증 (가입 X, Redis 상태 변경)
    @Override
    public void verifyEmail(String email, String code) {
        redisEmailVerificationService.verifyCode(email, code);
    }

    // 3. 최종 회원가입 (여기서 Redis 증표 확인)
    @Override
    public UserSignUpResponse signUp(UserSignUpRequest request) {

        redisEmailVerificationService.checkIsVerified(request.getEmail());

        Email email = Email.from(request.getEmail());
        validateDuplicateEmail(email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.signUp(
                email,
                encodedPassword,
                request.getNickname()
        );
        User savedUser = userRepository.save(user);

        redisEmailVerificationService.deleteVerifiedStatus(request.getEmail());

        return UserSignUpResponse.from(savedUser);
    }

    private void validateDuplicateEmail(Email email) {
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
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
        return UserProfileResponse.from(user);
    }

    @Override
    public UserWithdrawResponse withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.withdraw();

        redisUserContextService.evict(userId);
        redisTokenService.deleteRefreshTokenHash(userId);

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
        redisTokenService.deleteRefreshTokenHash(userId);
    }

    /***
     *아직 프론트 link 안 나옴
     */
    @Override
    public void requestPasswordReset(String emailValue) {
        Email email = Email.from(emailValue);

        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            String rawToken = redisPasswordResetTokenService. createAndSaveToken(emailValue);
            String link = "http://localhost:8080/api/user/password-reset/confirm?token=" + rawToken;

            log.info("link = {}", link);
            eventPublisher.publishEvent(new PasswordResetEvent(emailValue, link));
        });
    }

    @Override
    public void resetPassword(PasswordResetRequest request) {
        String emailValue = redisPasswordResetTokenService.validateAndGetEmail(request.getToken());

        User user = userRepository.findByEmailAndDeletedFalse(Email.from(emailValue))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        redisTokenService.deleteRefreshTokenHash(user.getId());
        redisUserContextService.evict(user.getId());

        redisPasswordResetTokenService.deleteToken(request.getToken());
    }


}
