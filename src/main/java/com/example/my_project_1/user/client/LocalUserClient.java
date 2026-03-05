package com.example.my_project_1.user.client;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LocalUserClient implements UserClient {

    private final Clock clock;
    private final UserRepository userRepository;

    private static final String UNKNOWN_USER = "(탈퇴한 사용자)";
    private static final String SUSPENDED_USER = "(차단된 사용자)";

    @Override
    public Map<Long, UserSummary> findUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> new UserSummary(user.getId(), resolveDisplayName(user))
                ));
    }

    private String resolveDisplayName(User user) {
        if (user.isWithdrawnCompletely() || user.isDeleted()) {
            return UNKNOWN_USER;
        }

        if (user.isSuspended(LocalDateTime.now(clock))) {
            return SUSPENDED_USER;
        }

        return user.getNickname();
    }
}
