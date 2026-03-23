package com.example.my_project_1.outbox.repository;

import com.example.my_project_1.outbox.domain.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
                SELECT o.id FROM OutboxEvent o
                WHERE o.status IN ('PENDING', 'FAILED')
                  AND o.nextRetryAt <= :now
                ORDER BY o.id ASC
            """)
    List<Long> findProcessableIds(LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("""
                UPDATE OutboxEvent o
                SET o.status = 'PROCESSING'
                WHERE o.id = :id
                  AND o.status IN ('PENDING', 'FAILED')
            """)
    int claim(@Param("id") Long id);
}
