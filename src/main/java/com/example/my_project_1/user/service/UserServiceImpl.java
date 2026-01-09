package com.example.my_project_1.user.service;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import com.example.my_project_1.user.service.request.UserSignUpRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUserContextService redisUserContextService;

    @PostConstruct
    void init() {
        User superUser = User.createSuperUser(passwordEncoder.encode("super"));
        userRepository.save(superUser);
    }


    @Transactional
    @Override
    public User signUp(UserSignUpRequest request) {
        Email email = new Email(request.getEmail());

        validateDuplicateEmail(email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.signUp(
                email,
                encodedPassword,
                request.getNickname()
        );

        return userRepository.save(user);
    }

    private void validateDuplicateEmail(Email email) {
        if (userRepository.existsByEmailAndDeletedFalse(email)) {
            throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }
    }

    @Transactional
    @Override
    public void suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.suspend();

        redisUserContextService.evict(userId);
    }
}
