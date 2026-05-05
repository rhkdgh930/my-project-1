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

    @Override
    public Map<Long, AuthorSummary> findAuthorsByIds(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::toAuthorSummary
                ));
    }

    private AuthorSummary toAuthorSummary(User user) {
        if (user.isWithdrawnCompletely() || user.isDeleted()) {
            return AuthorSummary.withdrawn();
        }

        if (user.isSuspended(LocalDateTime.now(clock))) {
            return AuthorSummary.suspended(user.getId());
        }

        return AuthorSummary.active(user.getId(), user.getNickname());
    }

}
