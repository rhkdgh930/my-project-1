package com.example.my_project_1.user.client;

import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LocalUserClient implements UserClient {

    private final UserRepository userRepository;

    private static final String UNKNOWN_USER = "(탈퇴한 사용자)";
    private static final String SUSPENDED_USER = "(차단된 사용자)";

    @Override
    public Map<Long, UserSummary> findUsersByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> {
                            if (user.isDeleted()) {
                                return new UserSummary(user.getId(), UNKNOWN_USER);
                            }

                            if (user.isSuspended()) {
                                return new UserSummary(user.getId(), SUSPENDED_USER);
                            }

                            return new UserSummary(user.getId(), user.getNickname());
                        }
                ));
    }
}
