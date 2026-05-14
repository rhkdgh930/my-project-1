package com.example.my_project_1.outbox.service;

import com.example.my_project_1.common.config.QueryDslConfig;
import com.example.my_project_1.outbox.domain.OutboxEventType;
import com.example.my_project_1.outbox.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:outbox-publisher-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@Import({
        QueryDslConfig.class,
        OutboxPublisher.class,
        OutboxEventInsertService.class,
        OutboxPublisherIntegrationTest.TestClockConfig.class
})
class OutboxPublisherIntegrationTest {

    @jakarta.annotation.Resource
    private OutboxPublisher outboxPublisher;

    @jakarta.annotation.Resource
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("publishIfAbsent는 중복 eventKey를 false로 흡수하고 트랜잭션 rollback-only를 남기지 않는다.")
    void publishIfAbsent_duplicateEventKeyDoesNotLeakUnexpectedRollback() {
        String eventKey = "dormancy-notify:user:100";

        boolean first = outboxPublisher.publishIfAbsent(OutboxEventType.DORMANCY_NOTIFY, "{}", eventKey);
        boolean second = outboxPublisher.publishIfAbsent(OutboxEventType.DORMANCY_NOTIFY, "{}", eventKey);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThatCode(outboxRepository::findAll).doesNotThrowAnyException();
        assertThat(outboxRepository.findAll())
                .filteredOn(event -> eventKey.equals(event.getEventKey()))
                .hasSize(1);
    }

    @TestConfiguration
    static class TestClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(
                    Instant.parse("2026-05-11T01:02:03Z"),
                    ZoneId.of("Asia/Seoul")
            );
        }
    }
}
