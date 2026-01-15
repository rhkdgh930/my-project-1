package com.example.my_project_1.user.service;

import com.example.my_project_1.auth.service.RedisTokenService;
import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.ProfileDetail;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.request.UserProfileUpdateRequest;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import com.example.my_project_1.user.service.response.UserProfileResponse;
import com.example.my_project_1.user.service.response.UserSignUpResponse;
import com.example.my_project_1.user.service.response.UserWithdrawResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUserContextService redisUserContextService;
    private final RedisTokenService redisTokenService;

    @Override
    public UserSignUpResponse signUp(UserSignUpRequest request) {
        Email email = Email.from(request.getEmail());
        validateDuplicateEmail(email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.signUp(
                email,
                ProfileDetail.defaultProfile(),
                encodedPassword,
                request.getNickname()
        );

        return UserSignUpResponse.from(userRepository.save(user));
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


}
