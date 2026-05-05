package com.example.my_project_1.outbox.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventKeyTest {

    @Test
    @DisplayName("postCreated는 post id 기반의 deterministic key를 사용한다.")
    void postCreated_usesDeterministicPostIdKey() {
        String firstKey = OutboxEventKey.postCreated(10L);
        String secondKey = OutboxEventKey.postCreated(10L);

        assertThat(firstKey).isEqualTo("POST_CREATED:10");
        assertThat(secondKey).isEqualTo("POST_CREATED:10");
    }

    @Test
    @DisplayName("postUpdated는 post id와 uuid 기반 key를 사용한다.")
    void postUpdated_usesPostIdAndUuidKey() {
        String eventKey = OutboxEventKey.postUpdated(10L);
        String[] parts = eventKey.split(":");

        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isEqualTo("POST_UPDATED");
        assertThat(parts[1]).isEqualTo("10");
        assertThat(UUID.fromString(parts[2])).isNotNull();
    }

    @Test
    @DisplayName("postUpdated는 연속 호출 시 서로 다른 key를 생성한다.")
    void postUpdated_createsDifferentKeysForConsecutiveCalls() {
        String firstKey = OutboxEventKey.postUpdated(10L);
        String secondKey = OutboxEventKey.postUpdated(10L);

        assertThat(firstKey).isNotEqualTo(secondKey);
    }
}
