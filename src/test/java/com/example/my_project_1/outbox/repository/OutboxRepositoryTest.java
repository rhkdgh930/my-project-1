package com.example.my_project_1.outbox.repository;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:outbox-repository-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class OutboxRepositoryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("처리 가능한 PENDING/FAILED 이벤트 id만 nextRetryAt 순서로 조회한다.")
    void findProcessableIds_returnsPendingAndFailedReadyEvents() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent readyPending = event("ready-pending", now.minusSeconds(1));
        OutboxEvent futurePending = event("future-pending", now.plusMinutes(1));
        OutboxEvent readyFailed = event("ready-failed", now.minusSeconds(2));
        readyFailed.markFail(new RuntimeException("temporary"), now.minusMinutes(1));
        ReflectionTestUtils.setField(readyFailed, "nextRetryAt", now.minusSeconds(2));
        OutboxEvent success = event("success", now.minusSeconds(3));
        success.markSuccess(now.minusSeconds(2));
        outboxRepository.saveAllAndFlush(List.of(readyPending, futurePending, readyFailed, success));

        List<Long> ids = outboxRepository.findProcessableIds(now, PageRequest.ofSize(10));

        assertThat(ids).containsExactly(readyFailed.getId(), readyPending.getId());
    }

    @Test
    @DisplayName("claim은 처리 가능한 이벤트를 PROCESSING으로 바꾼다.")
    void claim_updatesReadyEventToProcessing() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = outboxRepository.saveAndFlush(event("claim-success", now.minusSeconds(1)));

        int updated = outboxRepository.claim(event.getId(), now);
        OutboxEvent reloaded = outboxRepository.findById(event.getId()).orElseThrow();

        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(reloaded.getLastTriedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("claim은 PROCESSING/SUCCESS/DEAD 이벤트를 다시 claim하지 않는다.")
    void claim_rejectsNonProcessableStatuses() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent processing = outboxRepository.saveAndFlush(event("processing", now.minusSeconds(1)));
        outboxRepository.claim(processing.getId(), now);
        OutboxEvent success = event("success", now.minusSeconds(1));
        success.markSuccess(now);
        OutboxEvent dead = event("dead", now.minusSeconds(1));
        dead.markDead("dead", now);
        outboxRepository.saveAllAndFlush(List.of(success, dead));

        assertThat(outboxRepository.claim(processing.getId(), now.plusSeconds(1))).isZero();
        assertThat(outboxRepository.claim(success.getId(), now.plusSeconds(1))).isZero();
        assertThat(outboxRepository.claim(dead.getId(), now.plusSeconds(1))).isZero();
    }

    @Test
    @DisplayName("claim은 nextRetryAt이 미래인 이벤트를 claim하지 않는다.")
    void claim_rejectsFutureRetryEvent() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent event = outboxRepository.saveAndFlush(event("future", now.plusMinutes(1)));

        int updated = outboxRepository.claim(event.getId(), now);

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("findStuckProcessingIds는 threshold 이전 PROCESSING 이벤트만 조회한다.")
    void findStuckProcessingIds_returnsOldProcessingEvents() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent stuck = outboxRepository.saveAndFlush(event("stuck", now.minusMinutes(10)));
        outboxRepository.claim(stuck.getId(), now.minusMinutes(10));
        OutboxEvent recent = outboxRepository.saveAndFlush(event("recent", now.minusMinutes(1)));
        outboxRepository.claim(recent.getId(), now.minusMinutes(1));

        List<Long> ids = outboxRepository.findStuckProcessingIds(
                now.minusMinutes(5),
                PageRequest.ofSize(10)
        );

        assertThat(ids).containsExactly(stuck.getId());
    }

    @Test
    @DisplayName("deleteSuccessBefore는 threshold 이전 SUCCESS 이벤트만 삭제한다.")
    void deleteSuccessBefore_deletesOldSuccessEvents() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 10, 0);
        OutboxEvent oldSuccess = event("old-success", now.minusDays(10));
        oldSuccess.markSuccess(now.minusDays(9));
        OutboxEvent recentSuccess = event("recent-success", now.minusDays(1));
        recentSuccess.markSuccess(now.minusHours(1));
        OutboxEvent oldFailed = event("old-failed", now.minusDays(10));
        oldFailed.markFail(new RuntimeException("temporary"), now.minusDays(9));
        outboxRepository.saveAllAndFlush(List.of(oldSuccess, recentSuccess, oldFailed));

        int deleted = outboxRepository.deleteSuccessBefore(now.minusDays(7));
        entityManager.flush();
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(outboxRepository.findById(oldSuccess.getId())).isEmpty();
        assertThat(outboxRepository.findById(recentSuccess.getId())).isPresent();
        assertThat(outboxRepository.findById(oldFailed.getId())).isPresent();
    }

    private OutboxEvent event(String eventKey, LocalDateTime now) {
        return OutboxEvent.create(
                OutboxEventType.USER_ACCOUNT_CHANGED,
                "{}",
                eventKey,
                now
        );
    }
}
