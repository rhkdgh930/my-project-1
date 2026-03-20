package com.example.my_project_1.outbox.repository;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
                SELECT o FROM OutboxEvent o
                WHERE o.status IN ('PENDING', 'FAILED')
                  AND o.nextRetryAt <= :now
                ORDER BY o.id ASC
            """)
    List<OutboxEvent> findProcessableEvents(LocalDateTime now, Pageable pageable);
}
