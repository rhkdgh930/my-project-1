package com.example.my_project_1.user.client;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.SuspensionReason;
import com.example.my_project_1.user.domain.SuspensionType;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalUserClientTest {

    private static final String PROFILE_IMAGE_URL = "/images/550e8400-e29b-41d4-a716-446655440000.png";

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneId.of("UTC")
    );
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LocalUserClient userClient = new LocalUserClient(clock, userRepository);

    @Test
    @DisplayName("AuthorSummary.suspended는 마스킹된 표시 이름을 반환한다.")
    void suspendedAuthorSummary_usesMaskedDisplayName() {
        AuthorSummary author = AuthorSummary.suspended(1L);

        assertThat(author.id()).isEqualTo(1L);
        assertThat(author.displayName()).isEqualTo("차단된 사용자");
        assertThat(author.status()).isEqualTo(AuthorStatus.SUSPENDED);
    }

    @Test
    @DisplayName("작성자 조회는 활성 사용자의 nickname과 ACTIVE 상태를 반환한다.")
    void findAuthorsByIds_returnsActiveAuthor() {
        User user = user(1L, "active@example.com", "active");
        user.updateProfile("hello", PROFILE_IMAGE_URL);
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        Map<Long, AuthorSummary> authors = userClient.findAuthorsByIds(List.of(1L));

        AuthorSummary author = authors.get(1L);
        assertThat(author.id()).isEqualTo(1L);
        assertThat(author.displayName()).isEqualTo("active");
        assertThat(author.status()).isEqualTo(AuthorStatus.ACTIVE);
        assertThat(author.profileImageUrl()).isEqualTo(PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("작성자 조회는 탈퇴 완료 사용자의 마스킹 nickname을 노출하지 않는다.")
    void findAuthorsByIds_hidesWithdrawnUserMaskedNickname() {
        User user = user(1L, "withdrawn@example.com", "nickname");
        user.requestWithdrawal(LocalDateTime.parse("2026-01-01T00:00:00"));
        user.completeWithdrawal();
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        Map<Long, AuthorSummary> authors = userClient.findAuthorsByIds(List.of(1L));

        AuthorSummary author = authors.get(1L);
        assertThat(author.id()).isNull();
        assertThat(author.displayName()).isEqualTo("탈퇴한 사용자");
        assertThat(author.displayName()).doesNotContain("알수없음_");
        assertThat(author.status()).isEqualTo(AuthorStatus.WITHDRAWN);
        assertThat(author.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("작성자 조회는 차단 사용자의 nickname과 SUSPENDED 상태를 반환한다.")
    void findAuthorsByIds_returnsSuspendedAuthor() {
        User user = user(1L, "suspended@example.com", "suspended");
        user.suspend(
                SuspensionType.TEMPORARY,
                SuspensionReason.OTHER,
                Duration.ofDays(1),
                LocalDateTime.parse("2026-01-01T00:00:00")
        );
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));

        Map<Long, AuthorSummary> authors = userClient.findAuthorsByIds(List.of(1L));

        AuthorSummary author = authors.get(1L);
        assertThat(author.id()).isEqualTo(1L);
        assertThat(author.displayName()).isEqualTo("차단된 사용자");
        assertThat(author.status()).isEqualTo(AuthorStatus.SUSPENDED);
        assertThat(author.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("UNKNOWN author의 profileImageUrl은 null이다.")
    void unknownAuthorSummary_hasNoProfileImageUrl() {
        AuthorSummary author = AuthorSummary.unknown();

        assertThat(author.id()).isNull();
        assertThat(author.status()).isEqualTo(AuthorStatus.UNKNOWN);
        assertThat(author.profileImageUrl()).isNull();
    }

    private User user(Long userId, String email, String nickname) {
        User user = User.signUp(
                Email.from(email),
                "encodedPassword",
                nickname,
                LocalDateTime.parse("2026-01-01T00:00:00")
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
