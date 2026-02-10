package com.example.my_project_1.user.utils;

import com.example.my_project_1.auth.service.RedisUserContextService;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DormantUserJob {

    private final UserRepository userRepository;
    private final RedisUserContextService redisUserContextService;

    @Transactional
    @Scheduled(cron = "0 0 4 * * *") // 새벽 4시
    public void markDormantUsers() {

        LocalDateTime now = LocalDateTime.now();

        List<User> users =
                userRepository.findAllByUserStatus(UserStatus.ACTIVE);

        for (User user : users) {
            if (user.getDormancy() != null &&
                    user.getDormancy().shouldDormant(now)) {

                user.markDormant();
                redisUserContextService.evict(user.getId());

                log.info("[DormantUser] userId={}", user.getId());
            }
        }
    }
}

